package com.billiard.billiardapplication.Controller;

import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.billiard.billiardapplication.Service.TableService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.Duration;
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
        try {
            if (durationSpinner != null) {
                durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
                durationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());
            }

            if (minuteSpinner != null) {
                minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
                minuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());
            }
            if (paymentMethodCombo != null) {
                paymentMethodCombo.getItems().addAll("BCA (Debit)", "Cash", "Credit Card", "E-Wallet");
                paymentMethodCombo.setValue("BCA (Debit)");
            }
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
            String displayName = getTableDisplayName(table);
            if (tableTypeLabel != null) {
                tableTypeLabel.setText(displayName);
            }
            if (!table.isAvailable()) {
                setControlsForOccupiedTable();

                if (saveButton != null) {
                    saveButton.setText("Batalkan Sewa");
                    saveButton.setDisable(false);
                }
                if (table.getRent() != null && table.getRent().getCustomer() != null) {
                    if (customerNameField != null) {
                        customerNameField.setText(table.getRent().getCustomer().getCustomerName());
                    }
                    if (customerPhoneNumberField != null) {
                        customerPhoneNumberField.setText(table.getRent().getCustomer().getPhoneNumber());
                    }
                }
                if (table.getRent() != null) {
                    Duration remainingTime = table.getRent().getRemainingTime();
                    long minutes = remainingTime.toMinutes();
                    if (priceLabel != null) {
                        priceLabel.setText("Sisa waktu: " + minutes + " menit");
                    }
                }
            } else {
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
        if (customerNameField != null) customerNameField.setDisable(true);
        if (customerPhoneNumberField != null) customerPhoneNumberField.setDisable(true);
        if (durationSpinner != null) durationSpinner.setDisable(true);
        if (minuteSpinner != null) minuteSpinner.setDisable(true);
        if (paymentMethodCombo != null) paymentMethodCombo.setDisable(true);
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
        if (!table.isAvailable()) {
            return;
        }

        try {
            int hours = durationSpinner.getValue() != null ? durationSpinner.getValue() : 0;
            int minutes = minuteSpinner.getValue() != null ? minuteSpinner.getValue() : 0;
            int totalMinutes = hours * 60 + minutes;
            double hourlyRate = table instanceof VipTable ? 25000 : 15000;
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
        if (isProcessing) {
            return;
        }

        try {
            isProcessing = true;
            saveButton.setDisable(true);
            if (!table.isAvailable()) {
                handleCancelRental();
            } else {
                handleNewRental();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred while processing: " + e.getMessage());
        } finally {
            isProcessing = false;
            saveButton.setDisable(false);
        }
    }

    private void handleCancelRental() {
        try {
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
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

        if (cleanNumber.isEmpty()) {
            return false;
        }
        return cleanNumber.matches("^(\\+62|62|0)8[0-9]{8,11}$") ||
                cleanNumber.matches("^[0-9]{10,13}$");
    }

    private void showAlert(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (alert.getDialogPane().getScene().getWindow() instanceof Stage alertStage) {
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