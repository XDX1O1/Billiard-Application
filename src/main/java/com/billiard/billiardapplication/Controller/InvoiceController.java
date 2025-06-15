package com.billiard.billiardapplication.Controller;

import com.billiard.billiardapplication.App;
import com.billiard.billiardapplication.Entity.Renting.Invoice;
import com.billiard.billiardapplication.Repository.InvoiceRepositoryImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvoiceController {

    @FXML
    private Button backButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button userButton;

    @FXML
    private TableView<Invoice> invoiceTable;

    @FXML
    private TableColumn<Invoice, Integer> noColumn;

    @FXML
    private TableColumn<Invoice, String> customerNameColumn;

    @FXML
    private TableColumn<Invoice, String> phoneNumberColumn;

    @FXML
    private TableColumn<Invoice, String> dateColumn;

    @FXML
    private TableColumn<Invoice, String> invoiceIdColumn;

    @FXML
    private TableColumn<Invoice, String> tableTypeColumn;

    @FXML
    private TableColumn<Invoice, Double> amountColumn;

    @FXML
    private TableColumn<Invoice, String> paymentMethodColumn;

    private InvoiceRepositoryImpl invoiceRepository;
    private ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("InvoiceController initialize() called");

        // Set up table columns
        setupTableColumns();

        // Get invoice repository from App
        invoiceRepository = App.getInvoiceRepository();

        // Load data after UI is initialized
        Platform.runLater(this::loadInvoiceData);
    }

    private void setupTableColumns() {
        // Set up the table columns with proper property bindings
        noColumn.setCellValueFactory(cellData -> {
            int index = invoiceTable.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.SimpleIntegerProperty(index).asObject();
        });

        invoiceIdColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        phoneNumberColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        // Format date column
        dateColumn.setCellValueFactory(cellData -> {
            String formattedDate = cellData.getValue().getRentalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(formattedDate);
        });

        tableTypeColumn.setCellValueFactory(new PropertyValueFactory<>("tableType"));

        // Format amount column
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(col -> new TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("Rp %.0f", amount));
                }
            }
        });

        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        // Set the data to the table
        invoiceTable.setItems(invoiceList);
    }

    private void loadInvoiceData() {
        try {
            if (invoiceRepository != null) {
                System.out.println("Loading invoice data...");
                List<Invoice> invoices = invoiceRepository.findAll();

                // Debug: Print all loaded invoices
                System.out.println("Raw invoices from database:");
                for (Invoice invoice : invoices) {
                    System.out.println("ID: " + invoice.getInvoiceId() +
                            ", Customer: " + invoice.getCustomerName() +
                            ", Date: " + invoice.getRentalDate());
                }

                // Check for duplicates before adding to list
                Set<String> seenIds = new HashSet<>();
                List<Invoice> uniqueInvoices = new ArrayList<>();

                for (Invoice invoice : invoices) {
                    if (!seenIds.contains(invoice.getInvoiceId())) {
                        seenIds.add(invoice.getInvoiceId());
                        uniqueInvoices.add(invoice);
                    } else {
                        System.out.println("Duplicate invoice detected: " + invoice.getInvoiceId());
                    }
                }

                invoiceList.clear();
                invoiceList.addAll(uniqueInvoices);

                System.out.println("Loaded " + uniqueInvoices.size() + " unique invoices (filtered from " + invoices.size() + " total)");

                // Force table refresh
                Platform.runLater(() -> {
                    invoiceTable.refresh();
                });

            } else {
                System.err.println("Invoice repository is null");
                showAlert("Error", "Unable to load invoice data. Repository not initialized.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load invoice data: " + e.getMessage());
        }
    }

    @FXML
    void handleBackAction(ActionEvent event) {
        try {
            // Navigate back to Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/billiard/billiardapplication/Dashboard.fxml"));
            Parent root = loader.load();

            // Get the dashboard controller and set the table service
            DashboardController dashboardController = loader.getController();
            if (dashboardController != null) {
                dashboardController.setTableService(App.getTableService());
            }

            // Get current stage and set new scene
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not navigate back to dashboard: " + e.getMessage());
        }
    }

    @FXML
    void handleDownloadAction(ActionEvent event) {
        if (invoiceList.isEmpty()) {
            showAlert("No Data", "No invoices available to download.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Invoice Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("invoice_report.csv");

        Stage stage = (Stage) downloadButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                exportToCSV(file);
                showAlert("Export Successful", "Invoice report exported successfully to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Export Error", "Failed to export invoice report: " + e.getMessage());
            }
        }
    }

    private void exportToCSV(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write CSV header
            writer.append("No,Invoice ID,Customer Name,Phone Number,Date,Table Type,Amount,Payment Method\n");

            // Write data rows
            for (int i = 0; i < invoiceList.size(); i++) {
                Invoice invoice = invoiceList.get(i);
                writer.append(String.valueOf(i + 1)).append(",");
                writer.append(escapeCSV(invoice.getInvoiceId())).append(",");
                writer.append(escapeCSV(invoice.getCustomerName())).append(",");
                writer.append(escapeCSV(invoice.getPhoneNumber())).append(",");
                writer.append(escapeCSV(invoice.getRentalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))).append(",");
                writer.append(escapeCSV(invoice.getTableType())).append(",");
                writer.append(String.format("%.0f", invoice.getAmount())).append(",");
                writer.append(escapeCSV(invoice.getPaymentMethod())).append("\n");
            }
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma or quote
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void showAlert(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Set the alert to be always on top
            if (alert.getDialogPane().getScene().getWindow() instanceof Stage) {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alertStage.setAlwaysOnTop(true);
            }

            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to refresh invoice data (can be called from other controllers)
    public void refreshData() {
        loadInvoiceData();
    }

    public void cleanupDuplicates() {
        try {
            if (invoiceRepository != null) {
                invoiceRepository.removeDuplicateInvoices();
                loadInvoiceData(); // Reload after cleanup
                showAlert("Success", "Duplicate invoices removed successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to cleanup duplicates: " + e.getMessage());
        }
    }

    @FXML
    private void handleUserButtonAction(ActionEvent event) {
        try {
            System.out.println("User button clicked from Invoice screen - attempting to close application");

            // Get current window
            Window window = userButton.getScene().getWindow();

            // Show confirmation dialog
            boolean shouldExit = App.showExitConfirmation(window);

            if (shouldExit) {
                // Clean up current controller resources first
                cleanup();

                // Perform application shutdown
                App.shutdown();
            } else {
                System.out.println("Exit cancelled by user");
            }

        } catch (Exception e) {
            System.err.println("Error in handleUserButtonAction: " + e.getMessage());
            e.printStackTrace();

            // Fallback - force close if there's an error
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
        System.out.println("Cleaning up InvoiceController resources...");

        try {
            // Clear observable list
            if (invoiceList != null) {
                invoiceList.clear();
            }
            System.out.println("Invoice list cleared");
        } catch (Exception e) {
            System.err.println("Error clearing invoice list: " + e.getMessage());
        }

        try {
            // Clear table selection
            if (invoiceTable != null) {
                invoiceTable.getSelectionModel().clearSelection();
            }
            System.out.println("Table selection cleared");
        } catch (Exception e) {
            System.err.println("Error clearing table selection: " + e.getMessage());
        }

        System.out.println("InvoiceController cleanup completed");
    }
}