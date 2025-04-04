/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package finalproject.serverfinal;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;

public class CryptoUtils {
    private static final String AES = "AES";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom secureRandom = new SecureRandom();

    // Generate random master secret
    public static String generateMasterSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    // Derive keys from master secret
    public static SecretKey[] deriveKeys(String masterSecret) throws Exception {
        byte[] masterBytes = Base64.getDecoder().decode(masterSecret);
        
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] encKeyBytes = sha256.digest(("ENC" + masterSecret).getBytes());
        byte[] macKeyBytes = sha256.digest(("MAC" + masterSecret).getBytes());

        return new SecretKey[]{
            new SecretKeySpec(encKeyBytes, 0, 16, AES),
            new SecretKeySpec(macKeyBytes, HMAC_SHA256)
        };
    }

    // Unified encryption method (handles both PSK String and SecretKey)
    public static String encryptData(Object key, String data) throws Exception {
        Cipher cipher = Cipher.getInstance(AES);
        
        if (key instanceof String) {
            // PSK encryption
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(((String)key).getBytes(), AES));
        } else if (key instanceof SecretKey) {
            // Session key encryption
            cipher.init(Cipher.ENCRYPT_MODE, (SecretKey)key);
        } else {
            throw new IllegalArgumentException("Key must be String (PSK) or SecretKey");
        }
        
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    // Unified decryption method (handles both PSK String and SecretKey)
    public static String decryptData(Object key, String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(AES);
        
        if (key instanceof String) {
            // PSK decryption
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(((String)key).getBytes(), AES));
        } else if (key instanceof SecretKey) {
            // Session key decryption
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey)key);
        } else {
            throw new IllegalArgumentException("Key must be String (PSK) or SecretKey");
        }
        
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
    }

    // MAC generation (unchanged)
    public static String generateMAC(String data, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(key);
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
    }

    // MAC verification (unchanged)
    public static boolean verifyMAC(String data, String receivedMAC, SecretKey key) throws Exception {
        return generateMAC(data, key).equals(receivedMAC);
    }
}