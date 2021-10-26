package de.srlabs.patchanalysis_module.util;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import de.srlabs.patchanalysis_module.analysis.TestUtils;

/***
 * This class contains method which are commonly used when sending HTTP request to the backend server
 */
public class HTTPRequestUtil {

    public static String generateBoundary() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[16];
        sr.nextBytes(random);
        return TestUtils.byteArrayToHex(random);
    }

    public static String readErrorResponse(InputStream errorStream) throws IOException {
        // Read response
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(errorStream)));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        return stringBuilder.toString();
    }

    public static String encryptData(byte[] data) {
        SecretKey key = getSymmetricKey();
        String encryptedData = encryptWithSymmetricKey(key, data);
        String encryptedSymmetricKey = encryptWithPublicKey(key.getEncoded());

        Map<String, String> result = new HashMap<>();
        result.put("data", encryptedData);
        result.put("key", encryptedSymmetricKey);

        String encrypted = new JSONObject(result).toString();

        return encrypted;
    }


    public static SecretKey getSymmetricKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey secKey = generator.generateKey();
            return secKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encryptWithSymmetricKey(SecretKey key, byte[] data) {
        // Based on: https://owasp.org/www-community/Using_the_Java_Cryptographic_Extensions
        try {
            // (1) Generate a cryptographically strong random 16-byte initialization vector
            SecureRandom random = new SecureRandom();
            byte initVector[] = new byte[16];
            random.nextBytes(initVector);
            IvParameterSpec iv = new IvParameterSpec(initVector);

            // (2) Prepare the key
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

            // (3) Prepare the AES Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

            // (4) Encode the plaintext as array of Bytes
            byte[] cipherBytes = cipher.doFinal(data);

            // (5) Build the output message initVector + cipherBytes -> base64
            byte[] messageBytes = new byte[initVector.length + cipherBytes.length];
            System.arraycopy(initVector, 0, messageBytes, 0, 16);
            System.arraycopy(cipherBytes, 0, messageBytes, 16, cipherBytes.length);

            return android.util.Base64.encodeToString(messageBytes, android.util.Base64.DEFAULT);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException | BadPaddingException |
                IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encryptWithPublicKey(byte[] data) {
        PublicKey key = getPublicKey();
        try {
            // https://developer.android.com/guide/topics/security/cryptography#java
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new OAEPParameterSpec("SHA-256",
                    "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] encryptedBytes = cipher.doFinal(data);
            return android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT);
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                NoSuchPaddingException | NoSuchAlgorithmException |
                InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PublicKey getPublicKey() {
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAmIqqvscSMQugkbyACdeA\n" +
                "e/w//OsFcFDnm16OdIgkUue+N6E4XPisgXYENQesbuNGnZqdK1ondUQiKYsFGwiR\n" +
                "HwStJshsVm9lJyEn6lCXMCn2Wcfr2CiP8p7MVpoa8TOxZSiNI7zboVAAm1/SdDbh\n" +
                "4BoHXMxxoE4r1fd+LEYUgPOt0y8x4kZieVISvQp4jom7cgDrqnH+de6R6dILj54U\n" +
                "FPqvewQsg1MneuKWv47Tqsg3CFs/FbfKLJdo45P6GDL0EWMT3cYbj9HjcMJ0t5lM\n" +
                "byIGX76YSHJnhVzFZqkpA8ECT9MNjD28jChL+bLiReG/cYHZiO+k1qSM8K9TZIBg\n" +
                "HsZlAJ+kamHz27gJe4xIrZKTyome12ehvqlk0+O9jV7OI4vqTiSR8G34X8Z20HCE\n" +
                "tx6PY3Zr0KJm11MXs12FEdUXwRwPqOL3jgDvEbTdfACRw8ILo1LK3qcAFTC3nRWs\n" +
                "8iXFZkXhDEmaQOUVlAmwi1jMebRm8sp6N1hzEPknMPmii8p5JFtNdOfd//A574gZ\n" +
                "ACz1KRd92GuxwAmeS00q0c3FFZkdcApXfPYCaxyQrH5TnE1w8Dl3y4AM/JkX5Lfx\n" +
                "DeJeBlj20f/NNLC+vn0a04TGsJd09qXVCWTd5vnLSbGw+g6sO8ZXbRxjAH1RLWRa\n" +
                "wJsSu06LiE0cQXRzY95vGAECAwEAAQ==\n" +
                "-----END PUBLIC KEY-----";
        publicKey = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] byteKey = android.util.Base64.decode(publicKey, android.util.Base64.DEFAULT);
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
