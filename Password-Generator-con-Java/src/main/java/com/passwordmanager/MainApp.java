package com.passwordmanager;

import com.passwordmanager.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
       // Inicia DB al arrancar
       DatabaseManager.Initialize();

       FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/passwordmanager/views/login.fxml")
       );
       Scene scene = new Scene(loader.load(), 400, 300);
       stage.setTitle("Password Manager");
       stage.setScene(scene);
       stage.setResizable(false);
       stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}