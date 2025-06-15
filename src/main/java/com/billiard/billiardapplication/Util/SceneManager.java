package com.billiard.billiardapplication.Util;

import com.billiard.billiardapplication.App;
import com.billiard.billiardapplication.Controller.DashboardController;
import com.billiard.billiardapplication.Service.TableServiceImpl;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SceneManager {

    public static void switchScene(Stage stage, String fxmlPath) {
        try {
            System.out.println("Attempting to load FXML: " + fxmlPath);
            URL fxmlUrl = SceneManager.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: " + fxmlPath);
                System.err.println("Make sure the file exists in src/main/resources" + fxmlPath);
                return;
            }

            System.out.println("FXML URL found: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            if (fxmlPath.contains("Dashboard")) {
                DashboardController dashboardController = loader.getController();
                TableServiceImpl tableService = App.getTableService();

                if (dashboardController != null && tableService != null) {
                    dashboardController.setTableService(tableService);
                } else {
                    System.err.println("Dashboard controller or table service is null");
                }
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            System.out.println("Successfully switched to scene: " + fxmlPath);

        } catch (IOException e) {
            System.err.println("IOException while loading FXML: " + fxmlPath);
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "No cause"));
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error while switching scene: " + fxmlPath);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static <T> T switchSceneWithController(Stage stage, String fxmlPath, T controller) {
        try {
            System.out.println("Attempting to load FXML with custom controller: " + fxmlPath);

            URL fxmlUrl = SceneManager.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: " + fxmlPath);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            System.out.println("Successfully switched to scene with controller: " + fxmlPath);
            return controller;

        } catch (IOException e) {
            System.err.println("Failed to switch scene with controller to: " + fxmlPath);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}