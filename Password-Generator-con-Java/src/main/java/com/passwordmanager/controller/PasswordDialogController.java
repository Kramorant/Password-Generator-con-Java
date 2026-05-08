package com.passwordmanager.controller;

import com.passwordmanager.dao.PasswordDao;
import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.util.CryptoUtil;
import com.passwordmanager.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ResourceBundle;

public class PasswordDialogController implements Initializable {

    @FXML private TextField     titleField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Slider        lengthSlider;
    @FXML private Label         lengthLabel;
    @FXML private CheckBox      chkUppercase;
    @FXML private CheckBox      chkNumbers;
    @FXML private CheckBox      chkSymbols;
    @FXML private Label         errorLabel;
    @FXML private Button        saveButton;

    private PasswordEntry existingEntry; // null si es nueva entrada

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS   = "0123456789";
    private static final String SYMBOLS   = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final SecureRandom secureRandom = new SecureRandom();

    // -------------------------------------------------------------------------
    // Inicialización
    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Sincronizar el label de longitud con el slider
        lengthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            lengthLabel.setText(String.valueOf(newVal.intValue()))
        );
    }

    /**
     * Llamado desde MainController para pasar la entrada a editar.
     * Si entry es null, el diálogo opera en modo creación.
     * @param entry PasswordEntry a editar, o null para nueva entrada.
     */
    public void setEntry(PasswordEntry entry) {
        this.existingEntry = entry;

        if (entry != null) {
            titleField.setText(entry.getTitle());
            usernameField.setText(entry.getUsername());

            // Desciframos la contraseña para mostrarla en el campo
            try {
                String decrypted = CryptoUtil.decrypt(
                    entry.getEncrypted(),
                    SessionManager.getInstance().getAesKey()
                );
                passwordField.setText(decrypted);
            } catch (Exception e) {
                showError("Error al descifrar la contraseña.");
            }

            saveButton.setText("Actualizar");
        }
    }

    // -------------------------------------------------------------------------
    // Generador de contraseñas
    // -------------------------------------------------------------------------

    @FXML
    private void handleGenerate() {
        int length = (int) lengthSlider.getValue();

        // Construimos el alfabeto según las opciones seleccionadas
        StringBuilder alphabet = new StringBuilder(LOWERCASE);
        if (chkUppercase.isSelected()) alphabet.append(UPPERCASE);
        if (chkNumbers.isSelected())   alphabet.append(NUMBERS);
        if (chkSymbols.isSelected())   alphabet.append(SYMBOLS);

        if (alphabet.length() == 0) {
            showError("Selecciona al menos un tipo de carácter.");
            return;
        }

        // Generamos la contraseña
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(alphabet.length());
            password.append(alphabet.charAt(index));
        }

        passwordField.setText(password.toString());
        clearError();
    }

    // -------------------------------------------------------------------------
    // Guardar / Actualizar
    // -------------------------------------------------------------------------

    @FXML
    private void handleSave() {
        String title    = titleField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validaciones
        if (title.isEmpty()) {
            showError("El título no puede estar vacío.");
            return;
        }
        if (password.isEmpty()) {
            showError("La contraseña no puede estar vacía.");
            return;
        }

        try {
            // Ciframos la contraseña con la clave AES de la sesión
            String encrypted = CryptoUtil.encrypt(
                password,
                SessionManager.getInstance().getAesKey()
            );

            if (existingEntry == null) {
                // Nueva entrada
                PasswordEntry newEntry = new PasswordEntry(title, username, encrypted, "");
                PasswordDao.save(newEntry);
            } else {
                // Actualizar entrada existente
                existingEntry.setTitle(title);
                existingEntry.setUsername(username);
                existingEntry.setEncrypted(encrypted);
                PasswordDao.update(existingEntry);
            }

            closeDialog();

        } catch (Exception e) {
            showError("Error al cifrar la contraseña: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Cancelar
    // -------------------------------------------------------------------------

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private void closeDialog() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}