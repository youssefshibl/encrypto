package com.encrypservice;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

/**
 * SymmetricEncryption
 */
public class SymmetricEncryption {

    KeyGenerator keyGen;
    SecretKey secretKey;

    public void generateKey() {
        try {
            // generate key
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // AES key size can be 128, 192, or 256 bits
            secretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setKey(byte[] key) {
        secretKey = new SecretKeySpec(key, 0, key.length, "AES");
    }

    

    public byte[] encrypt(byte[] message) {
        try {
            // create cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(message);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // return null or an empty byte array in case of an error
        }
    }

    public byte[] decrypt(byte[] encryptedMessage) {
        try {
            // create cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // return null or an empty byte array in case of an error
        }
    }
}