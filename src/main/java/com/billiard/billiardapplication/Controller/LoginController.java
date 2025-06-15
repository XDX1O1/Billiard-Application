package com.billiard.billiardapplication.Controller;

import com.billiard.billiardapplication.Service.AdminService;
import com.billiard.billiardapplication.Util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import com.billiard.billiardapplication.Service.AdminServiceImpl;
import javafx.scene.input.MouseEvent;

public class LoginController {

    private final AdminService adminService;

    private double xOffset = 0;
    private double yOffset = 0;

    public LoginController(AdminServiceImpl adminService) {
        this.adminService = adminService;
    }

    @FXML
    private Button closeButton;

    @FXML
    private PasswordField inputPasswordField;

    @FXML
    private TextField inputUsernameField;

    @FXML
    private Button loginButton;

    @FXML
    private Label popupLoginText;

    @FXML
    private AnchorPane rootPane;

    @FXML
    public void initialize() {
        inputUsernameField.textProperty().addListener(
                (obs, oldText, newText)
                        -> popupLoginText.setText(""));
        inputPasswordField.textProperty().addListener(
                (obs, oldText, newText)
                        -> popupLoginText.setText(""));

        setupDragFunctionality();
    }

    private void setupDragFunctionality() {
        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> handleMousePressed(event));
            rootPane.setOnMouseDragged(event -> handleMouseDragged(event));
        }
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    public void loginButtonAction(ActionEvent event) {
        String username = inputUsernameField.getText();
        String password = inputPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            popupLoginText.setText("Username and Password cannot be empty.");
            return;
        }

        boolean login = adminService.login(username, password);

        if (login) {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            SceneManager.switchScene(stage, "/com/billiard/billiardapplication/Dashboard.fxml");
        } else {
            popupLoginText.setText("Invalid username or password.");
        }

    }

    @FXML
    public void closeButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
