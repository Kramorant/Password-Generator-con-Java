package com.passwordmanager.util;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

    // --- Configuración Argon2 ---
    private static final int ARGON2_TYPE       = Argon2Parameters.ARGON2_id;
    private static final int ARGON2_VERSION    = Argon2Parameters.ARGON2_VERSION_13;
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY     = 65536; // 64 MB
    private static final int ARGON2_PARALLELISM = 2;
    private static final int ARGON2_HASH_LENGTH = 32;   // 256 bits
    private static final int SALT_LENGTH        = 16;   // 128 bits

    // --- Configuración AES-256-GCM ---
    private static final int AES_KEY_LENGTH  = 256;
    private static final int GCM_IV_LENGTH   = 12;  // 96 bits (recomendado para GCM)
    private static final int GCM_TAG_LENGTH  = 128; // bits

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // ARGON2 — Hash de la contraseña maestra
    // -------------------------------------------------------------------------

    /**
     * Genera un salt aleatorio para usar con Argon2.
     * @return salt en Base64.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Genera el hash Argon2id de la contraseña maestra.
     * @param masterPassword contraseña maestra en texto plano.
     * @param saltBase64     salt en Base64 generado con generateSalt().
     * @return hash en Base64.
     */
    public static String hashMasterPassword(char[] masterPassword, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);

        Argon2Parameters params = new Argon2Parameters.Builder(ARGON2_TYPE)
                .withVersion(ARGON2_VERSION)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] passwordBytes = charArrayToByteArray(masterPassword);
        byte[] hash = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(passwordBytes, hash);

        // Limpiamos los bytes de la contraseña de memoria
        java.util.Arrays.fill(passwordBytes, (byte) 0);

        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifica si la contraseña maestra introducida coincide con el hash almacenado.
     * @param masterPassword contraseña a verificar.
     * @param saltBase64     salt almacenado en Base64.
     * @param storedHash     hash almacenado en Base64.
     * @return true si la contraseña es correcta.
     */
    public static boolean verifyMasterPassword(char[] masterPassword, String saltBase64, String storedHash) {
        String computedHash = hashMasterPassword(masterPassword, saltBase64);
        return computedHash.equals(storedHash);
    }

    // -------------------------------------------------------------------------
    // AES-256-GCM — Cifrado y descifrado de contraseñas
    // -------------------------------------------------------------------------

    /**
     * Deriva una clave AES-256 a partir del hash Argon2 de la contraseña maestra.
     * De esta forma la clave de cifrado nunca se almacena, se reconstruye en memoria.
     * @param hashBase64 hash Argon2 en Base64.
     * @return SecretKey AES-256.
     */
    public static SecretKey deriveAesKey(String hashBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(hashBase64);
        return new SecretKeySpec(keyBytes, "AES"); // 32 bytes = 256 bits
    }

    /**
     * Cifra un texto plano con AES-256-GCM.
     * @param plainText texto a cifrar.
     * @param key       clave AES-256 derivada de la contraseña maestra.
     * @return          texto cifrado en Base64.
     * @throws Exception si ocurre un error durante el cifrado.
     */
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Concatenamos IV + datos cifrados para almacenarlos juntos
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Descifra un texto cifrado con AES-256-GCM.
     * @param encryptedBase64 texto cifrado en Base64 (IV + datos).
     * @param key             clave AES-256 derivada de la contraseña maestra.
     * @return                texto descifrado.
     * @throws Exception si la clave es incorrecta o los datos están corruptos.
     */
    public static String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Separamos el IV de los datos cifrados
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, "UTF-8");
    }

    // -------------------------------------------------------------------------
    // Utilidades internas
    // -------------------------------------------------------------------------

    /**
     * Convierte un char[] a byte[] de forma segura (sin pasar por String).
     * Usar String intermedios expone la contraseña en el heap de la JVM.
     */
    private static byte[] charArrayToByteArray(char[] chars) {
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }
}