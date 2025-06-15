package com.billiard.billiardapplication.Util;

import com.billiard.billiardapplication.App;
import com.billiard.billiardapplication.Controller.DashboardController;
import com.billiard.billiardapplication.Service.TableServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneManager {

    private static final double SMALL_SCREEN_WIDTH = 1366;
    private static final double MEDIUM_SCREEN_WIDTH = 1600;
    private static final double LARGE_SCREEN_WIDTH = 1920;

    public static void switchScene(Stage stage, String fxmlPath) {
        switchScene(stage, fxmlPath, true);
    }

    public static void switchScene(Stage stage, String fxmlPath, boolean responsive) {
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
            if (responsive) {
                makeStageResponsive(stage, fxmlPath);
            }

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

    public static void makeStageResponsive(Stage stage, String fxmlPath) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        System.out.println("Screen dimensions: " + screenWidth + "x" + screenHeight);
        if (fxmlPath.toLowerCase().contains("login")) {
            stage.setResizable(false);
            return;
        }
        stage.setResizable(true);
        double stageWidth, stageHeight;

        if (screenWidth >= LARGE_SCREEN_WIDTH) {
            stageWidth = Math.min(1920, screenWidth * 0.95);
            stageHeight = Math.min(1080, screenHeight * 0.9);
        } else if (screenWidth >= MEDIUM_SCREEN_WIDTH) {
            stageWidth = screenWidth * 0.9;
            stageHeight = screenHeight * 0.85;
        } else if (screenWidth >= SMALL_SCREEN_WIDTH) {
            stageWidth = screenWidth * 0.95;
            stageHeight = screenHeight * 0.9;
        } else {
            stageWidth = screenWidth * 0.98;
            stageHeight = screenHeight * 0.95;
        }
        stage.setWidth(stageWidth);
        stage.setHeight(stageHeight);
        stage.setX((screenWidth - stageWidth) / 2);
        stage.setY((screenHeight - stageHeight) / 2);
        stage.setMinWidth(Math.min(1200, stageWidth * 0.8));
        stage.setMinHeight(Math.min(800, stageHeight * 0.8));
        if (screenWidth > 2000) {
            Platform.runLater(() -> stage.setMaximized(true));
        }

        System.out.println("Stage configured for responsive display: " + stageWidth + "x" + stageHeight);
    }

    public static <T> T switchSceneWithController(Stage stage, String fxmlPath, T controller) {
        return switchSceneWithController(stage, fxmlPath, controller, true);
    }

    public static <T> T switchSceneWithController(Stage stage, String fxmlPath, T controller, boolean responsive) {
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
            if (responsive) {
                makeStageResponsive(stage, fxmlPath);
            }

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

    public static double getScreenScaleFactor() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth();

        if (screenWidth >= LARGE_SCREEN_WIDTH) {
            return 1.0;
        } else if (screenWidth >= MEDIUM_SCREEN_WIDTH) {
            return screenWidth / LARGE_SCREEN_WIDTH;
        } else {
            return Math.max(0.7, screenWidth / LARGE_SCREEN_WIDTH);
        }
    }
}