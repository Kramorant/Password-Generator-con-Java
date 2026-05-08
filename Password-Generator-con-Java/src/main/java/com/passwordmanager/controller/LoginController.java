package com.passwordmanager.controller;

import com.passwordmanager.dao.MasterUserDao;
import com.passwordmanager.util.CryptoUtil;
import com.passwordmanager.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private Label        titleLabel;
    @FXML private PasswordField passwordField;
    @FXML private VBox          confirmBox;
    @FXML private PasswordField confirmField;
    @FXML private Label         errorLabel;
    @FXML private Button        actionButton;

    private boolean isRegisterMode;

    // -------------------------------------------------------------------------
    // Inicialización
    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        isRegisterMode = !MasterUserDao.masterUserExists();

        if (isRegisterMode) {
            titleLabel.setText("Crear contraseña maestra");
            actionButton.setText("Registrarse");
            confirmBox.setVisible(true);
            confirmBox.setManaged(true);
        } else {
            titleLabel.setText("Bienvenido de nuevo");
            actionButton.setText("Iniciar sesión");
            confirmBox.setVisible(false);
            confirmBox.setManaged(false);
        }
    }

    // -------------------------------------------------------------------------
    // Acción principal (registro o login)
    // -------------------------------------------------------------------------

    @FXML
    private void handleAction() {
        if (isRegisterMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    private void handleRegister() {
        char[] password = passwordField.getText().toCharArray();
        char[] confirm  = confirmField.getText().toCharArray();

        // Validaciones
        if (password.length == 0) {
            showError("La contraseña no puede estar vacía.");
            return;
        }
        if (password.length < 8) {
            showError("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        if (!java.util.Arrays.equals(password, confirm)) {
            showError("Las contraseñas no coinciden.");
            clearSensitiveFields(password, confirm);
            return;
        }

        try {
            // Generamos salt y hash con Argon2
            String salt = CryptoUtil.generateSalt();
            String hash = CryptoUtil.hashMasterPassword(password, salt);

            // Guardamos en base de datos
            MasterUserDao.saveMasterUser(hash, salt);

            // Guardamos el hash en sesión para derivar la clave AES
            SessionManager.getInstance().setMasterHash(hash);

            clearSensitiveFields(password, confirm);
            navigateToMain();

        } catch (Exception e) {
            showError("Error durante el registro: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    private void handleLogin() {
        char[] password = passwordField.getText().toCharArray();

        if (password.length == 0) {
            showError("Introduce tu contraseña maestra.");
            return;
        }

        try {
            String salt       = MasterUserDao.getSalt();
            String storedHash = MasterUserDao.getHash();

            if (CryptoUtil.verifyMasterPassword(password, salt, storedHash)) {
                // Guardamos el hash en sesión para derivar la clave AES
                SessionManager.getInstance().setMasterHash(storedHash);
                clearSensitiveFields(password);
                navigateToMain();
            } else {
                showError("Contraseña incorrecta. Inténtalo de nuevo.");
                clearSensitiveFields(password);
            }

        } catch (Exception e) {
            showError("Error durante el inicio de sesión: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Navegación
    // -------------------------------------------------------------------------

    private void navigateToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/passwordmanager/views/main.fxml")
            );
            Stage stage = (Stage) actionButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 500));
            stage.setTitle("Password Manager");
            stage.setResizable(true);
        } catch (Exception e) {
            showError("Error al cargar la pantalla principal.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearSensitiveFields(char[]... arrays) {
        for (char[] arr : arrays) {
            java.util.Arrays.fill(arr, '\0');
        }
        passwordField.clear();
        confirmField.clear();
    }
}