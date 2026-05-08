package com.passwordmanager.util;

import javax.crypto.SecretKey;

public class SessionManager {

    private static SessionManager instance;

    private String    masterHash;
    private SecretKey aesKey;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Almacena el hash y deriva la clave AES automáticamente.
     * @param masterHash hash Argon2 en Base64.
     */
    public void setMasterHash(String masterHash) {
        this.masterHash = masterHash;
        this.aesKey     = CryptoUtil.deriveAesKey(masterHash);
    }

    /**
     * Devuelve la clave AES activa de la sesión.
     * @return SecretKey lista para cifrar/descifrar.
     */
    public SecretKey getAesKey() {
        return aesKey;
    }

    /**
     * Limpia los datos de sesión al cerrar la aplicación o cerrar sesión.
     */
    public void clear() {
        masterHash = null;
        aesKey     = null;
    }
}