package com.passwordmanager.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:password_manager.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabla del usuario maestro
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS master_user (
                    id      INTEGER PRIMARY KEY,
                    hash    TEXT NOT NULL,
                    salt    TEXT NOT NULL
                )
            """);

            // Tabla de contraseñas almacenadas
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS passwords (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    title       TEXT NOT NULL,
                    username    TEXT,
                    encrypted   TEXT NOT NULL,
                    iv          TEXT NOT NULL,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            System.out.println("Base de datos inicializada correctamente.");

        } catch (SQLException e) {
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }
}