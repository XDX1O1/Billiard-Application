package com.billiard.billiardapplication;

import com.billiard.billiardapplication.Controller.LoginController;
import com.billiard.billiardapplication.Repository.AdminRepositoryImpl;
import com.billiard.billiardapplication.Repository.InvoiceRepositoryImpl;
import com.billiard.billiardapplication.Repository.TableRepositoryImpl;
import com.billiard.billiardapplication.Service.AdminServiceImpl;
import com.billiard.billiardapplication.Service.TableServiceImpl;
import com.billiard.billiardapplication.Service.TimerService;
import com.billiard.billiardapplication.Util.DatabaseUtil;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.Time;
import java.util.Optional;

public class App extends Application {

    private static HikariDataSource hikariDataSource;
    private static AdminServiceImpl adminService;
    private static TableServiceImpl tableService;
    private static InvoiceRepositoryImpl invoiceRepository; // Add invoice repository

    @Override
    public void start(Stage stage) throws IOException {

        hikariDataSource = DatabaseUtil.getHikariDataSource();

        // Initialize repositories
        AdminRepositoryImpl adminRepository = new AdminRepositoryImpl(hikariDataSource);
        TableRepositoryImpl tableRepository = new TableRepositoryImpl(hikariDataSource);
        invoiceRepository = new InvoiceRepositoryImpl(hikariDataSource); // Initialize invoice repository

        // Initialize services with proper dependencies
        adminService = new AdminServiceImpl(adminRepository);
        tableService = new TableServiceImpl(tableRepository, invoiceRepository); // Pass both repositories

        tableRepository.initializeTablesInDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
        LoginController controller = new LoginController(adminService);
        loader.setController(controller);

        Parent root = loader.load();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static AdminServiceImpl getAdminService() {
        return adminService;
    }

    public static TableServiceImpl getTableService() {
        return tableService;
    }

    public static InvoiceRepositoryImpl getInvoiceRepository() {
        return invoiceRepository;
    }

    public static HikariDataSource getHikariDataSource() {
        return hikariDataSource;
    }

    public static void main(String[] args) {
        launch();
    }

    public static void shutdown() {
        System.out.println("Initiating application shutdown...");

        try {
            // Stop TimerService if it exists
            TimerService timerService = TimerService.getInstance();
            if (timerService != null) {
                timerService.shutdown();
                System.out.println("TimerService shutdown completed");
            }
        } catch (Exception e) {
            System.err.println("Error shutting down TimerService: " + e.getMessage());
        }

        try {
            // Print current pool status before closing
            DatabaseUtil.printPoolStatus();

            // Close database connections using DatabaseUtil
            DatabaseUtil.closeAllConnections();

        } catch (Exception e) {
            System.err.println("Error in database cleanup: " + e.getMessage());
        }

        System.out.println("Application shutdown completed");

        // Exit the application
        Platform.exit();
        System.exit(0);
    }

    public static boolean showExitConfirmation(Window owner) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit Application");
            alert.setHeaderText("Are you sure you want to exit?");
            alert.setContentText("All active timers will be stopped and the application will close.");

            // Set the alert to be always on top and modal
            if (owner != null) {
                alert.initOwner(owner);
                alert.initModality(Modality.APPLICATION_MODAL);
            }

            ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(exitButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == exitButton;

        } catch (Exception e) {
            System.err.println("Error showing exit confirmation: " + e.getMessage());
            return true; // Default to allowing exit if dialog fails
        }
    }

    // Override the stop method to handle application closing
    @Override
    public void stop() throws Exception {
        System.out.println("JavaFX Application stop() method called");
        shutdown();
        super.stop();
    }

}