package com.passwordmanager.dao;

import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PasswordDao {

    // -------------------------------------------------------------------------
    // Crear
    // -------------------------------------------------------------------------

    /**
     * Guarda una nueva contraseña cifrada en la base de datos.
     * @param entry objeto PasswordEntry con los datos a guardar.
     */
    public static void save(PasswordEntry entry) {
        String sql = """
                INSERT INTO passwords (title, username, encrypted, iv)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getUsername());
            stmt.setString(3, entry.getEncrypted());
            stmt.setString(4, entry.getIv());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la contraseña", e);
        }
    }

    // -------------------------------------------------------------------------
    // Leer
    // -------------------------------------------------------------------------

    /**
     * Devuelve todas las contraseñas almacenadas.
     * Las contraseñas siguen cifradas, el descifrado lo hace el controlador.
     * @return lista de PasswordEntry.
     */
    public static List<PasswordEntry> findAll() {
        String sql = "SELECT * FROM passwords ORDER BY created_at DESC";
        List<PasswordEntry> entries = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                entries.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener las contraseñas", e);
        }

        return entries;
    }

    /**
     * Busca contraseñas cuyo título contenga el texto indicado (búsqueda parcial).
     * @param query texto a buscar en el título.
     * @return lista de PasswordEntry coincidentes.
     */
    public static List<PasswordEntry> findByTitle(String query) {
        String sql = "SELECT * FROM passwords WHERE title LIKE ? ORDER BY created_at DESC";
        List<PasswordEntry> entries = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                entries.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar contraseñas", e);
        }

        return entries;
    }

    /**
     * Busca una contraseña por su ID.
     * @param id identificador de la entrada.
     * @return PasswordEntry o null si no existe.
     */
    public static PasswordEntry findById(int id) {
        String sql = "SELECT * FROM passwords WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            return rs.next() ? mapRow(rs) : null;

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar la contraseña por ID", e);
        }
    }

    // -------------------------------------------------------------------------
    // Actualizar
    // -------------------------------------------------------------------------

    /**
     * Actualiza una entrada existente en la base de datos.
     * @param entry objeto PasswordEntry con los datos actualizados (debe tener ID).
     */
    public static void update(PasswordEntry entry) {
        String sql = """
                UPDATE passwords
                SET title = ?, username = ?, encrypted = ?, iv = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getUsername());
            stmt.setString(3, entry.getEncrypted());
            stmt.setString(4, entry.getIv());
            stmt.setInt(5, entry.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la contraseña", e);
        }
    }

    // -------------------------------------------------------------------------
    // Eliminar
    // -------------------------------------------------------------------------

    /**
     * Elimina una contraseña por su ID.
     * @param id identificador de la entrada a eliminar.
     */
    public static void delete(int id) {
        String sql = "DELETE FROM passwords WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la contraseña", e);
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades internas
    // -------------------------------------------------------------------------

    /**
     * Mapea una fila del ResultSet a un objeto PasswordEntry.
     */
    private static PasswordEntry mapRow(ResultSet rs) throws SQLException {
        return new PasswordEntry(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("username"),
                rs.getString("encrypted"),
                rs.getString("iv"),
                rs.getString("created_at")
        );
    }
}