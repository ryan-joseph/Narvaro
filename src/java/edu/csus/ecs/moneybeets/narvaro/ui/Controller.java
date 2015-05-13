/**
 * Narvaro: @VERSION@
 * Build Date: @DATE@
 * Commit Head: @HEAD@
 * JDK: @JDK@
 * ANT: @ANT@
 *
 */

package edu.csus.ecs.moneybeets.narvaro.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import edu.csus.ecs.moneybeets.narvaro.model.DataManager;
import edu.csus.ecs.moneybeets.narvaro.model.MonthData;
import edu.csus.ecs.moneybeets.narvaro.model.ParkMonth;
import edu.csus.ecs.moneybeets.narvaro.model.TimeSpan;
import edu.csus.ecs.moneybeets.narvaro.util.ConfigurationManager;
import edu.csus.ecs.moneybeets.narvaro.util.TaskEngine;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import javafx.util.Callback;
import org.apache.log4j.Logger;

public class Controller {

    private static final Logger LOG = Logger.getLogger(Controller.class.getName());

    /* Enter Data Tab Start */
    @FXML
    private Tab enterDataTab;
    @FXML
    private ComboBox<String> selectAParkDropDownMenu;
    @FXML
    private ComboBox<Integer> enterYear;
    @FXML
    private ComboBox<Month> enterMonth;
    @FXML
    private TextField conversionFactorPaidDayUseTF;
    @FXML
    private TextField paidDayUseTotalsTF;
    @FXML
    private TextField specialEventsTF;
    @FXML
    private TextField annualDayUseTF;
    @FXML
    private TextField dayUseTF;
    @FXML
    private TextField seniorTF;
    @FXML
    private TextField disabledTF;
    @FXML
    private TextField goldenBearTF;
    @FXML
    private TextField disabledVeteranTF;
    @FXML
    private TextField nonResOHVPassTF;
    @FXML
    private TextField annualPassSaleTF;
    @FXML
    private TextField campingTF;
    @FXML
    private TextField seniorCampingTF;
    @FXML
    private TextField disabledCampingTF;
    @FXML
    private TextField conversionFactorFreeDayUseTF;
    @FXML
    private TextField freeDayUseTotalsTF;
    @FXML
    private TextField totalVehiclesTF;
    @FXML
    private TextField totalPeopleTF;
    @FXML
    private TextField ratioTF;
    @FXML
    private TextArea commentsTB;
    @FXML
    private TextField mcTF;
    @FXML
    private TextField atvTF;
    @FXML
    private TextField fourByFourTF;
    @FXML
    private TextField rovTF;
    @FXML
    private TextField aqmaTF;
    @FXML
    private TextField allStarKartingTF;
    @FXML
    private TextField hangtownTF;
    @FXML
    private TextField otherTF;
    @FXML
    private Group userDataGroup;
    @FXML
    private Button clearButton;
    @FXML
    private Button submitButton;
    @FXML
    private ImageView submitButtonStatusIndicator;
    @FXML
    private Button browseFileButton;
    @FXML
    private TextField browseFileTF;
    /* Enter Data Tab End */

    /* View Data Tab Start */
    @FXML
    private Tab viewDataTab;
    @FXML
    private ComboBox<Month> monthSelectionOne;
    @FXML
    private ComboBox<Integer> yearSelectionOne;
    @FXML
    private ComboBox<Month> monthSelectionTwo;
    @FXML
    private ComboBox<Integer> yearSelectionTwo;
    @FXML
    private ListView<String> parkView;
    @FXML
    private Button searchButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private AnchorPane viewDataPane;
    @FXML
    private TableView viewDataTable;
    /* View Data Tab End */

    /* Graph Data Tab Start */
    @FXML
    private Tab graphDataTab;
    @FXML
    private DatePicker selectDateX;
    @FXML
    private DatePicker selectDateY;
    @FXML
    private ToggleGroup graphType;
    @FXML
    private Button viewDataButton;
    @FXML
    private Button view449FormButton;
    @FXML
    private Button graphButton;
    @FXML
    private Button printButton;
    @FXML
    private LineChart<?, ?> graphArea;
    @FXML
    private MenuButton selectParkOne;
    @FXML
    private MenuButton selectParkTwo;
    @FXML
    private MenuButton selectCategory;
    /* Graph Data Tab End */
    
    private Image okImage;
    private Image errorImage;
    private Image busyImage;
    
    @FXML
    public void initialize() {
        
        updateParkLists();

        // populate year field on enter data tab and view data tab
        LocalDateTime ldt = LocalDateTime.now();
        int year = ldt.getYear();
        for (; year >= 1984; year--) {
            enterYear.getItems().add(year);
            yearSelectionOne.getItems().add(year);
            yearSelectionTwo.getItems().add(year);
        }
        // populate month field on enter data tab
        enterMonth.getItems().addAll(Arrays.asList(Month.values()));

        // populate month fields on view data tab
        monthSelectionOne.getItems().addAll(Arrays.asList(Month.values()));
        monthSelectionTwo.getItems().addAll(Arrays.asList(Month.values()));

        // permit multiple selection on park listview
        parkView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    public void handleSubmitButton(final ActionEvent event) {
        // disable buttons so user don't press again
        submitButton.setDisable(true);
        clearButton.setDisable(true);
        browseFileButton.setDisable(true);
        // show busy spinner icon
        showBusyOnSubmit();
        // Submit processing to background task so we
        //   don't block the UI thread and freeze the window
        TaskEngine.INSTANCE.submit(new Runnable() {
            ParkMonth parkMonth = null;
            boolean success = false;

            @Override
            public void run() {
                if (validateEnteredData()) {
                    try {
                        parkMonth = new ParkMonth(getEnterPark());
                        parkMonth.createAndPutMonthData(YearMonth.of(getEnterYear(), getEnterMonth()),
                                getConversionFactorPaidDayUseTF(), getPaidDayUseTotalsTF(), getSpecialEventsTF(), getAnnualDayUseTF(),
                                getDayUseTF(), getSeniorTF(), getDisabledTF(), getGoldenBearTF(), getDisabledVeteranTF(),
                                getNonResOHVPassTF(), getAnnualPassSaleTF(), getCampingTF(), getSeniorCampingTF(),
                                getDisabledCampingTF(), getConversionFactorFreeDayUseTF(), getFreeDayUseTotalsTF(),
                                getTotalVehiclesTF(), getTotalPeopleTF(), getRatioTF(), getMcTF(), getAtvTF(), getFourByFourTF(),
                                getRovTF(), getAqmaTF(), getAllStarKartingTF(), getHangtownTF(), getOtherTF(), getCommentsTB(),
                                -1, getbrowseFile());
                        success = true;
                    } catch (Exception e) {
                        // something was wrong with data
                        LOG.error(e.getMessage(), e);
                        showErrorOnSubmit();
                    }

                    if (success) {
                        // attempt to write into database
                        success = false;
                        try {
                            DataManager.Narvaro.storeParkMonth(parkMonth);
                            success = true;
                        } catch (SQLException e) {
                            LOG.error(e.getMessage(), e);
                        }
                        if (success) {
                            showOKOnSubmit();
                            resetValidation();
                        } else {
                            showErrorOnSubmit();
                        }
                    }
                }
                // enable buttons again
                submitButton.setDisable(false);
                clearButton.setDisable(false);
                browseFileButton.setDisable(false);
            }
        });
    }
    
    private boolean validateEnteredData() {
        boolean ok = true;
        try {
            getEnterPark();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    selectAParkDropDownMenu.setStyle("-fx-background-color:#87D37C;");
                }
            });
        } catch (Exception e) {
            ok = false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    selectAParkDropDownMenu.setStyle("-fx-background-color:#EF4836;");
                }
            });
        }
        try {
            getEnterYear();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    enterYear.setStyle("-fx-background-color:#87D37C;");
                }
            });
        } catch (Exception e) {
            ok = false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    enterYear.setStyle("-fx-background-color:#EF4836;");
                }
            });
        }
        try {
            getEnterMonth();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    enterMonth.setStyle("-fx-background-color:#87D37C;");
                }
            });
        } catch (Exception e) {
            ok = false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    enterMonth.setStyle("-fx-background-color:#EF4836;");
                }
            });
        }
        try {
            getConversionFactorPaidDayUseTF();
            showValid(conversionFactorPaidDayUseTF);
        } catch (Exception e) {
            ok = false;
            showError(conversionFactorPaidDayUseTF);
        }
        try {
            getPaidDayUseTotalsTF();
            showValid(paidDayUseTotalsTF);
        } catch (Exception e) {
            ok = false;
            showError(paidDayUseTotalsTF);
        }
        try {
            getSpecialEventsTF();
            showValid(specialEventsTF);
        } catch (Exception e) {
            ok = false;
            showError(specialEventsTF);
        }
        try {
            getAnnualDayUseTF();
            showValid(annualDayUseTF);
        } catch (Exception e) {
            ok = false;
            showError(annualDayUseTF);
        }
        try {
            getDayUseTF();
            showValid(dayUseTF);
        } catch (Exception e) {
            ok = false;
            showError(dayUseTF);
        }
        try {
            getSeniorTF();
            showValid(seniorTF);
        } catch (Exception e) {
            ok = false;
            showError(seniorTF);
        }
        try {
            getDisabledTF();
            showValid(disabledTF);
        } catch (Exception e) {
            ok = false;
            showError(disabledTF);
        }
        try {
            getGoldenBearTF();
            showValid(goldenBearTF);
        } catch (Exception e) {
            ok = false;
            showError(goldenBearTF);
        }
        try {
            getDisabledVeteranTF();
            showValid(disabledVeteranTF);
        } catch (Exception e) {
            ok = false;
            showError(disabledVeteranTF);
        }
        try {
            getNonResOHVPassTF();
            showValid(nonResOHVPassTF);
        } catch (Exception e) {
            ok = false;
            showError(nonResOHVPassTF);
        }
        try {
            getAnnualPassSaleTF();
            showValid(annualPassSaleTF);
        } catch (Exception e) {
            ok = false;
            showError(annualPassSaleTF);
        }
        try {
            getCampingTF();
            showValid(campingTF);
        } catch (Exception e) {
            ok = false;
            showError(campingTF);
        }
        try {
            getSeniorCampingTF();
            showValid(seniorCampingTF);
        } catch (Exception e) {
            ok = false;
            showError(seniorCampingTF);
        }
        try {
            getDisabledCampingTF();
            showValid(disabledCampingTF);
        } catch (Exception e) {
            ok = false;
            showError(disabledCampingTF);
        }
        try {
            getConversionFactorFreeDayUseTF();
            showValid(conversionFactorFreeDayUseTF);
        } catch (Exception e) {
            ok = false;
            showError(conversionFactorFreeDayUseTF);
        }
        try {
            getFreeDayUseTotalsTF();
            showValid(freeDayUseTotalsTF);
        } catch (Exception e) {
            ok = false;
            showError(freeDayUseTotalsTF);
        }
        try {
            getTotalVehiclesTF();
            showValid(totalVehiclesTF);
        } catch (Exception e) {
            ok = false;
            showError(totalVehiclesTF);
        }
        try {
            getTotalPeopleTF();
            showValid(totalPeopleTF);
        } catch (Exception e) {
            ok = false;
            showError(totalPeopleTF);
        }
        try {
            getRatioTF();
            showValid(ratioTF);
        } catch (Exception e) {
            ok = false;
            showError(ratioTF);
        }
        try {
            getMcTF();
            showValid(mcTF);
        } catch (Exception e) {
            ok = false;
            showError(mcTF);
        }
        try {
            getAtvTF();
            showValid(atvTF);
        } catch (Exception e) {
            ok = false;
            showError(atvTF);
        }
        try {
            getFourByFourTF();
            showValid(fourByFourTF);
        } catch (Exception e) {
            ok = false;
            showError(fourByFourTF);
        }
        try {
            getRovTF();
            showValid(rovTF);
        } catch (Exception e) {
            ok = false;
            showError(rovTF);
        }
        try {
            getAqmaTF();
            showValid(aqmaTF);
        } catch (Exception e) {
            ok = false;
            showError(aqmaTF);
        }
        try {
            getAllStarKartingTF();
            showValid(allStarKartingTF);
        } catch (Exception e) {
            ok = false;
            showError(allStarKartingTF);
        }
        try {
            getHangtownTF();
            showValid(hangtownTF);
        } catch (Exception e) {
            ok = false;
            showError(hangtownTF);
        }
        try {
            getOtherTF();
            showValid(otherTF);
        } catch (Exception e) {
            ok = false;
            showError(otherTF);
        }
        try {
            File f = getbrowseFile();
            if (f.exists()) {
                showValid(browseFileTF);
            }
        } catch (Exception e) {
            ok = false;
            showError(browseFileTF);
        }
        if (ok == false) {
            showErrorOnSubmit();
        }
        return ok;  
    }
    
    private void resetValidation() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                selectAParkDropDownMenu.setStyle("-fx-background-color:#FFFFFF;");
                enterYear.setStyle("-fx-background-color:#FFFFFF;");
                enterMonth.setStyle("-fx-background-color:#FFFFFF;");
                resetValid(conversionFactorPaidDayUseTF);
                resetValid(paidDayUseTotalsTF);
                resetValid(specialEventsTF);
                resetValid(annualDayUseTF);
                resetValid(dayUseTF);
                resetValid(seniorTF);
                resetValid(disabledTF);
                resetValid(goldenBearTF);
                resetValid(disabledVeteranTF);
                resetValid(nonResOHVPassTF);
                resetValid(annualPassSaleTF);
                resetValid(campingTF);
                resetValid(seniorCampingTF);
                resetValid(disabledCampingTF);
                resetValid(conversionFactorFreeDayUseTF);
                resetValid(freeDayUseTotalsTF);
                resetValid(totalVehiclesTF);
                resetValid(totalPeopleTF);
                resetValid(ratioTF);
                resetValid(commentsTB);
                resetValid(mcTF);
                resetValid(atvTF);
                resetValid(fourByFourTF);
                resetValid(rovTF);
                resetValid(aqmaTF);
                resetValid(allStarKartingTF);
                resetValid(hangtownTF);
                resetValid(otherTF);
                resetValid(browseFileTF);
            }
        });
    }
    
    private void showValid(final Region r) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                r.setStyle("-fx-control-inner-background:#87D37C;");
            }
        });
    }
    
    private void showError(final Region r) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                r.setStyle("-fx-control-inner-background:#EF4836;");
            }
        });
    }
    
    private void resetValid(final Region r) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                r.setStyle("-fx-control-inner-background:#FFFFFF;");
            }
        });
    }
    
    public void showBusyOnSubmit() {
        InputStream in = null;
        if (busyImage == null) {
            try {
                in = new FileInputStream(ConfigurationManager.NARVARO.getHomeDirectory() 
                        + File.separator + "resources" + File.separator + "busy.gif");
                busyImage = new Image(in);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                submitButtonStatusIndicator.setImage(busyImage);
            }
        });
    }
    
    public void showOKOnSubmit() {
        InputStream in = null;
        if (okImage == null) {
            try {
                in = new FileInputStream(ConfigurationManager.NARVARO.getHomeDirectory() 
                        + File.separator + "resources" + File.separator + "ok.png");
                okImage = new Image(in);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                submitButtonStatusIndicator.setImage(okImage);
            }
        });
        TaskEngine.INSTANCE.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        clearSubmitButtonStatusIndicator();
                    }
                });
            }
        }, 2000);
        if (in != null) {
            try {
                in.close();
            } catch (Exception ex) {
                LOG.warn(ex.getMessage(), ex);
            }
        }
    }
    
    public void showErrorOnSubmit() {
        InputStream in = null;
        if (errorImage == null) {
            try {
                in = new FileInputStream(ConfigurationManager.NARVARO.getHomeDirectory() 
                        + File.separator + "resources" + File.separator + "error.png");
                errorImage = new Image(in);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                submitButtonStatusIndicator.setImage(errorImage);
            }
        });
        TaskEngine.INSTANCE.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        clearSubmitButtonStatusIndicator();
                    }
                });
            }
        }, 2000);
        if (in != null) {
            try {
                in.close();
            } catch (Exception ex) {
                LOG.warn(ex.getMessage(), ex);
            }
        }
    }
    
    private void clearSubmitButtonStatusIndicator() {
        try {
            submitButtonStatusIndicator.setImage(null);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
    
    @FXML
    public void handleBrowseButton(final ActionEvent event){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open 449 Form");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Excel Files", "*.xls,", "*.xlsx", "*.csv"));
        File file = fileChooser.showOpenDialog(browseFileButton.getScene().getWindow());

        if (file != null){
            String filePath = file.getPath();
            setbrowseFileTF(filePath);
        }
    }
    
    /**
     * Clears all data on-screen in text fields,
     * text-area's, and date-picker's.
     *
     * @param event The action event.
     */
    @FXML
    public void handleClearButton(final ActionEvent event) {
        Object[] o = userDataGroup.getChildren().toArray();
        for (int i = 0; i < o.length; i++) {
            if (o[i] instanceof TextField) {
                ((TextField) o[i]).clear();
            } else if (o[i] instanceof TextArea) {
                ((TextArea) o[i]).clear();
            } else if (o[i] instanceof DatePicker) {
                ((DatePicker) o[i]).setValue(null);
            }
        }
        selectAParkDropDownMenu.setValue(null);
        enterYear.setValue(null);
        enterMonth.setValue(null);
        resetValidation();
    }
    
    public void updateParkLists() {
        // get a list of all park names in the db
        List<String> parkNames = DataManager.Narvaro.getAllParkNames();
        // clear old items
        selectAParkDropDownMenu.getItems().clear();
        parkView.getItems().clear();
        // add park names to window
        for (String parkName : parkNames) {
            selectAParkDropDownMenu.getItems().add(parkName);
            parkView.getItems().add(parkName);
        }
    }

    @FXML
    public void handleSearchButton(final ActionEvent event) {
        int startYear = yearSelectionOne.getValue();
        Month startMonth = monthSelectionOne.getValue();
        int endYear = yearSelectionTwo.getValue();
        Month endMonth = monthSelectionTwo.getValue();
        TimeSpan ts = null;
        try {
            ts = DataManager.Narvaro.getTimeSpan(YearMonth.of(startYear, startMonth), YearMonth.of(endYear, endMonth));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        ObservableList<ObservableList> entries;
        entries = FXCollections.observableArrayList();

        Collection tempData;
        List<String> parkNames = parkView.getSelectionModel().getSelectedItems();

        for(String parkName : parkNames) {
            TableColumn col = new TableColumn();
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures, ObservableValue>() {
                @Override
                public ObservableValue call(TableColumn.CellDataFeatures param) {
                    return new SimpleStringProperty();
                }
            });
            viewDataTable.getColumns().addAll(col);
        }
        for(String parkName : parkNames) {
            tempData = ts.getParkMonth(parkName).getAllMonthData();
            Object data[] = tempData.toArray();
            ObservableList<String> row = FXCollections.observableArrayList();
            for(int i = 1; i < data.length; i++) {
                row.add(data[i].toString());
            }
            entries.add(row);
        }
        viewDataTable.setItems(entries);
    }

    /* Getter and Setter Forest. Abandon all hope, ye who enter */
    public String getEnterPark() {
        return selectAParkDropDownMenu.getSelectionModel().getSelectedItem().toString();
    }
    
    public int getEnterYear() {
        return Integer.parseInt(enterYear.getSelectionModel().getSelectedItem().toString());
    }
    
    public int getEnterMonth() {
        return enterMonth.getSelectionModel().getSelectedItem().getValue();
    }
    
    public BigDecimal getConversionFactorPaidDayUseTF() throws NumberFormatException {
        BigDecimal temp;
        try {
            temp = new BigDecimal(conversionFactorPaidDayUseTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + conversionFactorPaidDayUseTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setConversionFactorPaidDayUseTF(final String in) {
            conversionFactorPaidDayUseTF.setText(in);
    }
    
    public long getPaidDayUseTotalsTF() throws NumberFormatException {
        long temp;
        try {
            temp = Integer.parseInt(paidDayUseTotalsTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + paidDayUseTotalsTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setPaidDayUseTotalsTF(final String in) {
        paidDayUseTotalsTF.setText(in);
    }
    
    public int getSpecialEventsTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(specialEventsTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + specialEventsTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setSpecialEventsTF(final String in) {
        specialEventsTF.setText(in);
    }
    
    public int getAnnualDayUseTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(annualDayUseTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + annualDayUseTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setAnnualDayUseTF(final String in) {
        annualDayUseTF.setText(in);
    }
    
    public int getDayUseTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(dayUseTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + dayUseTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setDayUseTF(final String in) {
        dayUseTF.setText(in);
    }
    
    public int getSeniorTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(seniorTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + seniorTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setSeniorTF(final String in) {
        seniorTF.setText(in);
    }
    
    public int getDisabledTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(disabledTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + disabledTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setDisabledTF(final String in) {
        disabledTF.setText(in);
    }
    
    public int getGoldenBearTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(goldenBearTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + goldenBearTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setGoldenBearTF(final String in) {
        goldenBearTF.setText(in);
    }
    
    public int getDisabledVeteranTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(disabledVeteranTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + disabledVeteranTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setDisabledVeteranTF(final String in) {
        disabledVeteranTF.setText(in);
    }
    
    public int getNonResOHVPassTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(nonResOHVPassTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + nonResOHVPassTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setNonResOHVPassTF(final String in) {
        nonResOHVPassTF.setText(in);
    }
    
    public int getAnnualPassSaleTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(annualPassSaleTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + annualPassSaleTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setAnnualPassSaleTF(final String in) {
        annualDayUseTF.setText(in);
    }
    
    public int getCampingTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(campingTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + campingTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setCampingTF(final String in) {
        campingTF.setText(in);
    }
    
    public int getSeniorCampingTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(seniorCampingTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + seniorCampingTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setSeniorCampingTF(final String in) {
        seniorCampingTF.setText(in);
    }
    
    public int getDisabledCampingTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(disabledCampingTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + disabledCampingTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setDisabledCampingTF(final String in) {
        disabledCampingTF.setText(in);
    }
    
    public BigDecimal getConversionFactorFreeDayUseTF() throws NumberFormatException {
        BigDecimal temp;
        try {
            temp = new BigDecimal(conversionFactorFreeDayUseTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + conversionFactorFreeDayUseTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setConversionFactorFreeDayUseTF(final String in) {
        conversionFactorFreeDayUseTF.setText(in);
    }
    
    public int getFreeDayUseTotalsTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(freeDayUseTotalsTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + freeDayUseTotalsTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setFreeDayUseTotalsTF(final String in) {
        freeDayUseTotalsTF.setText(in);
    }
    
    public int getTotalVehiclesTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(totalVehiclesTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + totalVehiclesTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setTotalVehiclesTF(final String in) {
        totalVehiclesTF.setText(in);
    }
    
    public int getTotalPeopleTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(totalPeopleTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + totalPeopleTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setTotalPeopleTF(final String in) {
        totalPeopleTF.setText(in);
    }
    
    public BigDecimal getRatioTF() throws NumberFormatException {
        BigDecimal temp;
        try {
            temp = new BigDecimal(ratioTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + ratioTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setRatioTF(final String in) {
        ratioTF.setText(in);
    }
    
    public String getCommentsTB() {
        return commentsTB.getText();
    }
    
    public void setCommentsTB(final String targetText) {
        commentsTB.setText(targetText);
    }
    
    public int getMcTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(mcTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + mcTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setMcTF(final String in) {
        mcTF.setText(in);
    }
    
    public int getAtvTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(atvTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + atvTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setAtvTF(final String in) {
        atvTF.setText(in);
    }
    
    public int getFourByFourTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(fourByFourTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + fourByFourTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setFourByFourTF(final String in ) {
        fourByFourTF.setText(in);
    }
    
    public int getRovTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(rovTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + rovTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setRovTF(final String in) {
        rovTF.setText(in);
    }
    
    public int getAqmaTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(aqmaTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + aqmaTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setAqmaTF(final String in) {
        aqmaTF.setText(in);
    }
    
    public int getAllStarKartingTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(mcTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + mcTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setAllStarKartingTF(final String in) {
        allStarKartingTF.setText(in);
    }
    
    public int getHangtownTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(mcTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + mcTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setHangtownTF(final String in) {
        hangtownTF.setText(in);
    }
    
    public int getOtherTF() throws NumberFormatException {
        int temp = -1;
        try {
            temp = Integer.parseInt(otherTF.getText());
        } catch (NumberFormatException e) {
            LOG.error("Not a number: " + otherTF.getText());
            throw e;
        }
        return temp;
    }
    
    public void setOtherTF(final String in) {
        otherTF.setText(in);
    }
    
    public String getbrowseFileTF() {
        return browseFileTF.getText();
    }
    
    public void setbrowseFileTF(String in) {
        browseFileTF.setText(in);
    }
    
    public File getbrowseFile() {
        File file = new File(getbrowseFileTF());
        if(file.exists()) {
            return file;
        }
        else {
            /*
            Change field to red maybe?
             */
            return null;
        }
    }
    /* Enter Data Tab End */

    /* View Data Tab Start */
    public String getMonthSelectionOne() {
        return monthSelectionOne.getSelectionModel().getSelectedItem().toString();
    }
    
    public String getYearSelectionOne() {
        return yearSelectionOne.getSelectionModel().getSelectedItem().toString();
    }
    
    public String getMonthSelectionTwo() {
        return monthSelectionTwo.getSelectionModel().getSelectedItem().toString();
    }
    
    public String getYearSelectionTwo() {
        return yearSelectionTwo.getSelectionModel().getSelectedItem().toString();
    }
    /* View Data Tab End */

    /* Graph Data Tab Start */

    /* Graph Data Tab End */
}
