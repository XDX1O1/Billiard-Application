package com.billiard.billiardapplication.Controller;

import com.billiard.billiardapplication.App;
import com.billiard.billiardapplication.Entity.Table.NonVipTable;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.billiard.billiardapplication.Service.TableService;
import com.billiard.billiardapplication.Service.TableServiceImpl;
import com.billiard.billiardapplication.Service.TimerService;
import com.billiard.billiardapplication.Util.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    @FXML
    private ImageView logoImageView;
    @FXML
    private ComboBox<String> filterTypeCombo;
    @FXML
    private ComboBox<String> filterTableCombo;
    @FXML
    private Button docButton;
    @FXML
    private Button userButton;

    @FXML
    private GridPane tableGrid;

    @FXML
    private Label timeLabel1, timeLabel2, timeLabel3, timeLabel4, timeLabel5, timeLabel6,
            timeLabel7, timeLabel8, timeLabel9, timeLabel10, timeLabel11, timeLabel12;

    @FXML
    private Label nameLabel1, nameLabel2, nameLabel3, nameLabel4, nameLabel5, nameLabel6,
            nameLabel7, nameLabel8, nameLabel9, nameLabel10, nameLabel11, nameLabel12;

    private Map<Label, Integer> timerMap = new HashMap<>();
    private Map<VBox, Table> vboxTableMap = new HashMap<>();
    private Timeline timeline;
    private TableService tableService;
    private TimerService timerService;


    public void setTableService(TableService tableService) {
        this.tableService = tableService;
        this.timerService = TimerService.getInstance();
        if (tableService instanceof TableServiceImpl) {
            TableServiceImpl serviceImpl = (TableServiceImpl) tableService;
            timerService.setTableRepository(serviceImpl.getTableRepository());
        }
        Platform.runLater(this::initializeData);
    }

    @FXML
    public void initialize() {
        System.out.println("DashboardController initialize() called");
        if (filterTypeCombo != null) {
            filterTypeCombo.getItems().addAll("None", "VIP", "NON_VIP");
            filterTypeCombo.setValue("None");
        }

        if (filterTableCombo != null) {
            filterTableCombo.getItems().addAll("None", "Available", "Unavailable");
            filterTableCombo.setValue("None");
        }

        setupTimerLoop();
        Platform.runLater(this::setupTableClickHandlers);
    }

    public void initializeData() {
        System.out.println("initializeData() called, tableService: " + (tableService != null ? "not null" : "null"));

        if (tableService == null) {
            System.err.println("TableService is null in initializeData()");
            return;
        }

        if (filterTypeCombo != null) {
            filterTypeCombo.setOnAction(e -> {
                System.out.println("Type filter changed to: " + filterTypeCombo.getValue());
                refreshTableGrid();
            });
        }

        if (filterTableCombo != null) {
            filterTableCombo.setOnAction(e -> {
                System.out.println("Table filter changed to: " + filterTableCombo.getValue());
                refreshTableGrid();
            });
        }

        refreshTableGrid();
    }

    private void setupTableClickHandlers() {
        if (tableGrid == null) {
            System.err.println("TableGrid is null, cannot setup click handlers");
            return;
        }

        for (Node node : tableGrid.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                vbox.setOnMouseClicked(event -> {
                    Table table = vboxTableMap.get(vbox);
                    if (table != null) {
                        showTableRentalDialog(table);
                    }
                });
                vbox.setOnMouseEntered(event -> {
                    Table table = vboxTableMap.get(vbox);
                    if (table != null) {
                        vbox.setStyle(vbox.getStyle() + " -fx-opacity: 0.8; -fx-cursor: hand;");
                    }
                });

                vbox.setOnMouseExited(event -> {
                    Table table = vboxTableMap.get(vbox);
                    if (table != null) {
                        updateTableAppearanceWithCSS(vbox, table);
                        vbox.setStyle(vbox.getStyle().replace(" -fx-opacity: 0.8; -fx-cursor: hand;", ""));
                    }
                });
            }
        }
    }

    private void showTableRentalDialog(Table table) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/billiard/billiardapplication/TableRentalDialog.fxml"));
            Parent root = loader.load();
            TableRentalController controller = loader.getController();
            if (controller != null && tableService != null) {
                controller.setTableInfo(table, tableService);
            } else {
                System.err.println("Controller or TableService is null");
                return;
            }
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            if (tableGrid != null && tableGrid.getScene() != null && tableGrid.getScene().getWindow() != null) {
                modalStage.initOwner(tableGrid.getScene().getWindow());
            }

            modalStage.setTitle("Table Rental - " + getTableDisplayName(table));
            modalStage.setResizable(false);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.setScene(scene);
            modalStage.showAndWait();
            refreshTableGrid();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load TableRentalDialog.fxml: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error showing table rental dialog: " + e.getMessage());
        }
    }

    private void setupTimerLoop() {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimerDisplays();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        System.out.println("Timer display loop started successfully");
    }

    private void updateTimerDisplays() {
        boolean needsRefresh = false;

        for (Map.Entry<VBox, Table> entry : vboxTableMap.entrySet()) {
            VBox vbox = entry.getKey();
            Table table = entry.getValue();

            if (table != null && !table.isAvailable()) {
                int tableNumber = table.getTableNumber();

                if (timerService.hasActiveTimer(tableNumber)) {
                    int remainingSeconds = timerService.getRemainingSeconds(tableNumber);
                    Label timeLabel = findTimeLabelForTable(tableNumber);
                    if (timeLabel != null) {
                        Platform.runLater(() -> {
                            timeLabel.setText(formatTime(remainingSeconds));
                        });
                    }

                    if (remainingSeconds <= 0) {
                        needsRefresh = true;
                    }
                }
            }
        }

        if (needsRefresh) {
            Platform.runLater(() -> {
                System.out.println("Timer expired, refreshing grid...");
                refreshTableGrid();
            });
        }
    }

    private Label findTimeLabelForTable(int tableNumber) {
        for (int i = 1; i <= 12; i++) {
            VBox vbox = getVBoxAtPosition(i);
            if (vbox != null && vboxTableMap.containsKey(vbox)) {
                Table table = vboxTableMap.get(vbox);
                if (table != null && table.getTableNumber() == tableNumber) {
                    return getTimeLabel(i);
                }
            }
        }
        return null;
    }

    private void refreshTableGrid() {
        System.out.println("refreshTableGrid() called");

        if (tableService == null) {
            System.out.println("TableService is null, cannot refresh");
            return;
        }

        try {
            String typeFilter = filterTypeCombo != null ? filterTypeCombo.getValue() : "None";
            String availabilityFilter = filterTableCombo != null ? filterTableCombo.getValue() : "None";

            System.out.println("Current filters - Type: " + typeFilter + ", Availability: " + availabilityFilter);

            List<Table> filtered = tableService.getFilteredTables(availabilityFilter, typeFilter);
            System.out.println("Filtered tables count: " + filtered.size());
            vboxTableMap.clear();
            for (int i = 1; i <= 12; i++) {
                Label timeLabel = getTimeLabel(i);
                Label nameLabel = getNameLabel(i);
                VBox vbox = getVBoxAtPosition(i);

                if (timeLabel != null && nameLabel != null && vbox != null) {
                    timeLabel.setText("");
                    nameLabel.setText("");
                    if (i > filtered.size()) {
                        vbox.setVisible(false);
                    } else {
                        vbox.setVisible(true);
                    }
                }
            }
            for (int i = 0; i < Math.min(filtered.size(), 12); i++) {
                Table table = filtered.get(i);
                Label timeLabel = getTimeLabel(i + 1);
                Label nameLabel = getNameLabel(i + 1);
                VBox vbox = getVBoxAtPosition(i + 1);

                if (timeLabel != null && nameLabel != null && vbox != null && table != null) {
                    String displayName = getTableDisplayName(table);
                    nameLabel.setText(displayName);
                    vbox.setVisible(true);
                    vboxTableMap.put(vbox, table);
                    updateTableAppearanceWithCSS(vbox, table);

                    if (table.isAvailable()) {
                        timeLabel.setText("00:00");
                        timerService.stopTimer(table.getTableNumber());
                    } else {
                        try {
                            int remainingSeconds;
                            if (timerService.hasActiveTimer(table.getTableNumber())) {
                                remainingSeconds = timerService.getRemainingSeconds(table.getTableNumber());
                            } else {
                                remainingSeconds = (int) table.getRent().getRemainingTime().getSeconds();
                                timerService.startTimer(table.getTableNumber(), remainingSeconds);
                            }

                            timeLabel.setText(formatTime(remainingSeconds));
                            System.out.println("Set timer for table " + table.getTableNumber() +
                                    " at position " + (i + 1) + ": " + remainingSeconds + " seconds");

                        } catch (Exception e) {
                            System.err.println("Error getting remaining time for table " + table.getTableNumber() + ": " + e.getMessage());
                            timeLabel.setText("ERROR");
                        }
                    }
                }
            }
            Platform.runLater(this::setupTableClickHandlers);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error refreshing table grid: " + e.getMessage());
        }
    }

    private String getTableDisplayName(Table table) {
        if (table == null) return "Unknown";
        String type = table instanceof VipTable ? "VIP" : "Non-VIP";
        return type + " " + table.getTableNumber();
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Label getTimeLabel(int index) {
        return switch (index) {
            case 1 -> timeLabel1;
            case 2 -> timeLabel2;
            case 3 -> timeLabel3;
            case 4 -> timeLabel4;
            case 5 -> timeLabel5;
            case 6 -> timeLabel6;
            case 7 -> timeLabel7;
            case 8 -> timeLabel8;
            case 9 -> timeLabel9;
            case 10 -> timeLabel10;
            case 11 -> timeLabel11;
            case 12 -> timeLabel12;
            default -> null;
        };
    }

    private Label getNameLabel(int index) {
        return switch (index) {
            case 1 -> nameLabel1;
            case 2 -> nameLabel2;
            case 3 -> nameLabel3;
            case 4 -> nameLabel4;
            case 5 -> nameLabel5;
            case 6 -> nameLabel6;
            case 7 -> nameLabel7;
            case 8 -> nameLabel8;
            case 9 -> nameLabel9;
            case 10 -> nameLabel10;
            case 11 -> nameLabel11;
            case 12 -> nameLabel12;
            default -> null;
        };
    }

    private VBox getVBoxAtPosition(int index) {
        if (tableGrid == null) return null;

        try {
            for (Node node : tableGrid.getChildren()) {
                if (node instanceof VBox) {
                    Integer rowIndex = GridPane.getRowIndex(node);
                    Integer colIndex = GridPane.getColumnIndex(node);
                    int row = (rowIndex != null) ? rowIndex : 0;
                    int col = (colIndex != null) ? colIndex : 0;
                    int position = row * 3 + col + 1;

                    if (position == index) {
                        return (VBox) node;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting VBox at position " + index + ": " + e.getMessage());
        }
        return null;
    }

    @FXML
    private void handleUserButtonAction(ActionEvent event) {
        try {
            System.out.println("User button clicked - attempting to close application");
            Window window = userButton.getScene().getWindow();
            boolean shouldExit = App.showExitConfirmation(window);

            if (shouldExit) {
                cleanup();
                App.shutdown();
            } else {
                System.out.println("Exit cancelled by user");
            }

        } catch (Exception e) {
            System.err.println("Error in handleUserButtonAction: " + e.getMessage());
            e.printStackTrace();
            try {
                cleanup();
                App.shutdown();
            } catch (Exception fallbackError) {
                System.err.println("Fallback shutdown failed: " + fallbackError.getMessage());
                Platform.exit();
                System.exit(1);
            }
        }
    }

    public void cleanup() {
        System.out.println("Cleaning up DashboardController resources...");

        try {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
                System.out.println("Timeline stopped");
            }
        } catch (Exception e) {
            System.err.println("Error stopping timeline: " + e.getMessage());
        }

        try {
            if (vboxTableMap != null) {
                vboxTableMap.clear();
            }
            if (timerMap != null) {
                timerMap.clear();
            }
            System.out.println("Maps cleared");
        } catch (Exception e) {
            System.err.println("Error clearing maps: " + e.getMessage());
        }

        System.out.println("DashboardController cleanup completed");
    }

    @FXML
    private void handleDocButtonAction(ActionEvent event) {
        try {
            cleanup();
            Stage stage = (Stage) docButton.getScene().getWindow();
            SceneManager.switchScene(stage, "/com/billiard/billiardapplication/Invoice.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating to Invoice view: " + e.getMessage());
        }
    }

    private void updateTableAppearanceWithCSS(VBox vbox, Table table) {
        if (vbox == null || table == null) return;

        try {
            vbox.getStyleClass().removeAll("table-available", "table-occupied");
            if (table.isAvailable()) {
                vbox.getStyleClass().add("table-available");
            } else {
                vbox.getStyleClass().add("table-occupied");
            }
            for (Node child : vbox.getChildren()) {
                if (child instanceof Label) {
                    Label label = (Label) child;
                    label.getStyleClass().removeAll("table-label");
                    label.getStyleClass().add("table-label");
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating table appearance with CSS: " + e.getMessage());
        }
    }

}