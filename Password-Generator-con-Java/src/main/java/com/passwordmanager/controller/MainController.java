package com.passwordmanager.controller;

import com.passwordmanager.dao.PasswordDao;
import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.util.CryptoUtil;
import com.passwordmanager.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TextField                    searchField;
    @FXML private TableView<PasswordEntry>     passwordTable;
    @FXML private TableColumn<PasswordEntry, String> colTitle;
    @FXML private TableColumn<PasswordEntry, String> colUsername;
    @FXML private TableColumn<PasswordEntry, String> colCreated;
    @FXML private TableColumn<PasswordEntry, String> colActions;
    @FXML private Label                        statusLabel;

    private ObservableList<PasswordEntry> tableData;

    // -------------------------------------------------------------------------
    // Inicialización
    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupSearch();
        loadPasswords();
    }

    private void setupTableColumns() {
        colTitle.setCellValueFactory(
            cell -> new SimpleStringProperty(cell.getValue().getTitle())
        );
        colUsername.setCellValueFactory(
            cell -> new SimpleStringProperty(cell.getValue().getUsername())
        );
        colCreated.setCellValueFactory(
            cell -> new SimpleStringProperty(cell.getValue().getCreatedAt())
        );

        // Columna de acciones con botones Copiar / Editar / Eliminar
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCopy   = new Button("Copiar");
            private final Button btnEdit   = new Button("Editar");
            private final Button btnDelete = new Button("Eliminar");
            private final HBox   box       = new HBox(6, btnCopy, btnEdit, btnDelete);

            {
                btnCopy.setOnAction(e -> handleCopy(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));

                btnCopy.getStyleClass().add("secondary-button");
                btnEdit.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("danger-button");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                loadPasswords();
            } else {
                List<PasswordEntry> results = PasswordDao.findByTitle(newVal.trim());
                tableData.setAll(results);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Carga de datos
    // -------------------------------------------------------------------------

    private void loadPasswords() {
        List<PasswordEntry> entries = PasswordDao.findAll();
        tableData = FXCollections.observableArrayList(entries);
        passwordTable.setItems(tableData);
        setStatus(entries.size() + " contraseña(s) almacenada(s).");
    }

    // -------------------------------------------------------------------------
    // Nueva contraseña
    // -------------------------------------------------------------------------

    @FXML
    private void handleNew() {
        openPasswordDialog(null);
    }

    // -------------------------------------------------------------------------
    // Copiar contraseña al portapapeles
    // -------------------------------------------------------------------------

    private void handleCopy(PasswordEntry entry) {
        try {
            String decrypted = CryptoUtil.decrypt(
                entry.getEncrypted(),
                SessionManager.getInstance().getAesKey()
            );

            javafx.scene.input.Clipboard clipboard =
                javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content =
                new javafx.scene.input.ClipboardContent();
            content.putString(decrypted);
            clipboard.setContent(content);

            setStatus("Contraseña de \"" + entry.getTitle() + "\" copiada al portapapeles.");

        } catch (Exception e) {
            setStatus("Error al descifrar la contraseña.");
        }
    }

    // -------------------------------------------------------------------------
    // Editar contraseña
    // -------------------------------------------------------------------------

    private void handleEdit(PasswordEntry entry) {
        openPasswordDialog(entry);
    }

    // -------------------------------------------------------------------------
    // Eliminar contraseña
    // -------------------------------------------------------------------------

    private void handleDelete(PasswordEntry entry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar contraseña");
        alert.setHeaderText("¿Eliminar \"" + entry.getTitle() + "\"?");
        alert.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            PasswordDao.delete(entry.getId());
            loadPasswords();
            setStatus("Contraseña eliminada correctamente.");
        }
    }

    // -------------------------------------------------------------------------
    // Cerrar sesión
    // -------------------------------------------------------------------------

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/passwordmanager/views/login.fxml")
            );
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 300));
            stage.setTitle("Password Manager");
            stage.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Diálogo de nueva / edición de contraseña
    // -------------------------------------------------------------------------

    private void openPasswordDialog(PasswordEntry entry) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/passwordmanager/views/password_dialog.fxml")
            );

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(entry == null ? "Nueva contraseña" : "Editar contraseña");
            dialog.setScene(new Scene(loader.load(), 400, 340));
            dialog.setResizable(false);

            PasswordDialogController controller = loader.getController();
            controller.setEntry(entry);

            dialog.showAndWait();

            // Recargamos la tabla tras cerrar el diálogo
            loadPasswords();

        } catch (Exception e) {
            setStatus("Error al abrir el diálogo.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}