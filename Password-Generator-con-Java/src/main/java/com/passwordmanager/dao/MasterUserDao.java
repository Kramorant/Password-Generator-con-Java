package com.passwordmanager.dao;

import com.passwordmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MasterUserDao {

    // -------------------------------------------------------------------------
    // Comprobaciones
    // -------------------------------------------------------------------------

    /**
     * Comprueba si ya existe un usuario maestro registrado en la base de datos.
     * Útil para decidir si mostrar la pantalla de registro o la de login.
     * @return true si ya existe un usuario maestro.
     */
    public static boolean masterUserExists() {
        String sql = "SELECT COUNT(*) FROM master_user";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar el usuario maestro", e);
        }
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    /**
     * Guarda el hash y el salt de la contraseña maestra en la base de datos.
     * Solo se llama una vez, durante el registro inicial.
     * @param hash hash Argon2 en Base64.
     * @param salt salt en Base64.
     */
    public static void saveMasterUser(String hash, String salt) {
        if (masterUserExists()) {
            throw new IllegalStateException("Ya existe un usuario maestro registrado.");
        }

        String sql = "INSERT INTO master_user (hash, salt) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el usuario maestro", e);
        }
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Recupera el salt almacenado del usuario maestro.
     * Necesario para recalcular el hash durante la verificación del login.
     * @return salt en Base64, o null si no existe usuario.
     */
    public static String getSalt() {
        String sql = "SELECT salt FROM master_user LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getString("salt") : null;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el salt", e);
        }
    }

    /**
     * Recupera el hash almacenado del usuario maestro.
     * Necesario para verificar la contraseña maestra durante el login.
     * @return hash en Base64, o null si no existe usuario.
     */
    public static String getHash() {
        String sql = "SELECT hash FROM master_user LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getString("hash") : null;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el hash", e);
        }
    }
}