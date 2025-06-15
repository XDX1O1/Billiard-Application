package com.billiard.billiardapplication.Controller;

import com.billiard.billiardapplication.App;
import com.billiard.billiardapplication.Entity.Renting.Invoice;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.billiard.billiardapplication.Repository.InvoiceRepositoryImpl;
import com.billiard.billiardapplication.Service.TableService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TableRentalController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label tableTypeLabel;
    @FXML
    private TextField customerNameField;
    @FXML
    private TextField customerPhoneNumberField;
    @FXML
    private Spinner<Integer> durationSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private Label priceLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button closeButton;

    private Table table;
    private TableService tableService;
    private boolean isProcessing = false;

    @FXML
    private void initialize() {
        // Initialize spinners with proper error handling
        try {
            if (durationSpinner != null) {
                durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
                durationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());
            }

            if (minuteSpinner != null) {
                minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
                minuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());
            }

            // Initialize payment methods
            if (paymentMethodCombo != null) {
                paymentMethodCombo.getItems().addAll("BCA (Debit)", "Cash", "Credit Card", "E-Wallet");
                paymentMethodCombo.setValue("BCA (Debit)");
            }

            // Initialize price label
            if (priceLabel != null) {
                priceLabel.setText("Rp. 0");
            }

        } catch (Exception e) {
            System.err.println("Error initializing TableRentalController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setTableInfo(Table table, TableService tableService) {
        this.table = table;
        this.tableService = tableService;

        try {
            // Set table type display
            String displayName = getTableDisplayName(table);
            if (tableTypeLabel != null) {
                tableTypeLabel.setText(displayName);
            }

            // Check if table is available
            if (!table.isAvailable()) {
                // Table is occupied, show cancel option
                setControlsForOccupiedTable();

                if (saveButton != null) {
                    saveButton.setText("Batalkan Sewa"); // Change button text to "Cancel Rental"
                    saveButton.setDisable(false); // Enable the button for cancellation
                }

                // Show current customer info if available
                if (table.getRent() != null && table.getRent().getCustomer() != null) {
                    if (customerNameField != null) {
                        customerNameField.setText(table.getRent().getCustomer().getCustomerName());
                    }
                    if (customerPhoneNumberField != null) {
                        customerPhoneNumberField.setText(table.getRent().getCustomer().getPhoneNumber());
                    }
                }

                // Show remaining time info
                if (table.getRent() != null) {
                    Duration remainingTime = table.getRent().getRemainingTime();
                    long minutes = remainingTime.toMinutes();
                    if (priceLabel != null) {
                        priceLabel.setText("Sisa waktu: " + minutes + " menit");
                    }
                }
            } else {
                // Table is available, enable rental
                setControlsForAvailableTable();

                if (saveButton != null) {
                    saveButton.setText("Sewa");
                }
            }

            updatePrice();

        } catch (Exception e) {
            System.err.println("Error setting table info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setControlsForOccupiedTable() {
        // For occupied tables, disable input controls but show information
        if (customerNameField != null) customerNameField.setDisable(true);
        if (customerPhoneNumberField != null) customerPhoneNumberField.setDisable(true);
        if (durationSpinner != null) durationSpinner.setDisable(true);
        if (minuteSpinner != null) minuteSpinner.setDisable(true);
        if (paymentMethodCombo != null) paymentMethodCombo.setDisable(true);

        // Keep the save button enabled for cancellation
        if (saveButton != null) saveButton.setDisable(false);
    }

    private void setControlsEnabled(boolean enabled) {
        if (customerNameField != null) customerNameField.setDisable(!enabled);
        if (customerPhoneNumberField != null) customerPhoneNumberField.setDisable(!enabled);
        if (durationSpinner != null) durationSpinner.setDisable(!enabled);
        if (minuteSpinner != null) minuteSpinner.setDisable(!enabled);
        if (paymentMethodCombo != null) paymentMethodCombo.setDisable(!enabled);
        if (saveButton != null) saveButton.setDisable(!enabled);
    }

    private void setControlsForAvailableTable() {
        // For available tables, enable all controls
        if (customerNameField != null) customerNameField.setDisable(false);
        if (customerPhoneNumberField != null) customerPhoneNumberField.setDisable(false);
        if (durationSpinner != null) durationSpinner.setDisable(false);
        if (minuteSpinner != null) minuteSpinner.setDisable(false);
        if (paymentMethodCombo != null) paymentMethodCombo.setDisable(false);
        if (saveButton != null) saveButton.setDisable(false);
    }



    private void updatePrice() {
        if (table == null || durationSpinner == null || minuteSpinner == null || priceLabel == null) {
            return;
        }

        // Don't update price if table is occupied (showing remaining time instead)
        if (!table.isAvailable()) {
            return;
        }

        try {
            int hours = durationSpinner.getValue() != null ? durationSpinner.getValue() : 0;
            int minutes = minuteSpinner.getValue() != null ? minuteSpinner.getValue() : 0;
            int totalMinutes = hours * 60 + minutes;

            // Calculate price based on table type and duration
            double hourlyRate = table instanceof VipTable ? 25000 : 15000; // VIP: 25k, Non-VIP: 15k
            double totalPrice = (totalMinutes / 60.0) * hourlyRate;

            priceLabel.setText(String.format("Rp. %.0f", totalPrice));

        } catch (Exception e) {
            System.err.println("Error updating price: " + e.getMessage());
            if (priceLabel != null) {
                priceLabel.setText("Rp. Error");
            }
        }
    }

    private String getTableDisplayName(Table table) {
        if (table == null) return "Unknown Table";
        String type = table instanceof VipTable ? "VIP" : "Non-VIP";
        return type + " " + table.getTableNumber();
    }

    @FXML
    private void handleSaveButton() {
        // Prevent multiple clicks while processing
        if (isProcessing) {
            return;
        }

        try {
            isProcessing = true; // Set flag to prevent double processing
            saveButton.setDisable(true); // Disable button during processing

            // Check if this is a cancellation (table is occupied) or a new rental
            if (!table.isAvailable()) {
                handleCancelRental();
            } else {
                handleNewRental();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred while processing: " + e.getMessage());
        } finally {
            isProcessing = false; // Reset flag
            saveButton.setDisable(false); // Re-enable button
        }
    }

    private void handleCancelRental() {
        try {
            // Show confirmation dialog
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Pembatalan");
            confirmAlert.setHeaderText("Batalkan Penyewaan Meja?");

            String customerName = table.getRent() != null && table.getRent().getCustomer() != null
                    ? table.getRent().getCustomer().getCustomerName() : "Unknown";
            Duration remainingTime = table.getRent() != null ? table.getRent().getRemainingTime() : Duration.ZERO;
            long remainingMinutes = remainingTime.toMinutes();

            confirmAlert.setContentText(
                    "Apakah Anda yakin ingin membatalkan penyewaan meja ini?\n\n" +
                            "Customer: " + customerName + "\n" +
                            "Sisa waktu: " + remainingMinutes + " menit\n\n" +
                            "Tindakan ini tidak dapat dibatalkan."
            );

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // User confirmed cancellation
                boolean success = tableService.releaseTable(table.getTableNumber());

                if (success) {
                    showAlert("Berhasil", "Penyewaan meja berhasil dibatalkan!");
                    closeDialog();
                } else {
                    showAlert("Error", "Gagal membatalkan penyewaan meja. Silakan coba lagi.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan saat membatalkan penyewaan: " + e.getMessage());
        }
    }

    private void handleNewRental() {
        if (!validateInput()) {
            return;
        }

        try {
            String customerName = customerNameField.getText().trim();
            String phoneNumber = customerPhoneNumberField.getText().trim();
            int hours = durationSpinner.getValue() != null ? durationSpinner.getValue() : 0;
            int minutes = minuteSpinner.getValue() != null ? minuteSpinner.getValue() : 0;
            String paymentMethod = paymentMethodCombo.getValue();

            int totalMinutes = hours * 60 + minutes;

            if (totalMinutes <= 0) {
                showAlert("Error", "Please select a valid duration.");
                return;
            }

            // Use table service to rent the table with phone number
            boolean success = tableService.rentTable(table.getTableNumber(), customerName, phoneNumber, totalMinutes, paymentMethod);

            if (success) {
                showAlert("Success", "Table rented successfully!");
                closeDialog();
            } else {
                showAlert("Error", "Failed to rent table. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred while processing the rental: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (customerNameField == null || customerPhoneNumberField == null || tableService == null || table == null) {
            showAlert("System Error", "Required components are not initialized.");
            return false;
        }

        if (customerNameField.getText() == null || customerNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter customer name.");
            customerNameField.requestFocus();
            return false;
        }

        if (customerPhoneNumberField.getText() == null || customerPhoneNumberField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter phone number.");
            customerPhoneNumberField.requestFocus();
            return false;
        }

        // Basic phone number validation
        String phoneNumber = customerPhoneNumberField.getText().trim();
        if (!isValidPhoneNumber(phoneNumber)) {
            showAlert("Validation Error", "Please enter a valid phone number.");
            customerPhoneNumberField.requestFocus();
            return false;
        }

        if (paymentMethodCombo == null || paymentMethodCombo.getValue() == null) {
            showAlert("Validation Error", "Please select a payment method.");
            return false;
        }

        if (durationSpinner == null || minuteSpinner == null) {
            showAlert("System Error", "Duration controls are not initialized.");
            return false;
        }

        int totalMinutes = (durationSpinner.getValue() != null ? durationSpinner.getValue() : 0) * 60 +
                (minuteSpinner.getValue() != null ? minuteSpinner.getValue() : 0);

        if (totalMinutes <= 0) {
            showAlert("Validation Error", "Please select a valid duration (greater than 0 minutes).");
            return false;
        }

        return true;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Basic validation for Indonesian phone numbers
        // Allow numbers starting with 08, +62, or 62
        // Remove all non-digit characters for validation
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

        if (cleanNumber.isEmpty()) {
            return false;
        }

        // Check for valid Indonesian phone number patterns
        return cleanNumber.matches("^(\\+62|62|0)8[0-9]{8,11}$") ||
                cleanNumber.matches("^[0-9]{10,13}$");
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

    @FXML
    private void handleCloseButton() {
        closeDialog();
    }

    private void closeDialog() {
        try {
            if (closeButton != null && closeButton.getScene() != null && closeButton.getScene().getWindow() != null) {
                Stage stage = (Stage) closeButton.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}