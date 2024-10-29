package com.encrypservice;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.io.IOException;

public class GenerateKeys {

    public KeyPair keyPair;
    public java.security.PublicKey pubKey;
    public java.security.PrivateKey privKey;
    public byte[] pubKeyEncoded;
    public byte[] privKeyEncoded;

    public GenerateKeys() throws IOException {
        System.out.println("Generating keys...");
        try {
            keyPair = generateKeyPair();
            pubKey = keyPair.getPublic();
            privKey = keyPair.getPrivate();
            pubKeyEncoded = pubKey.getEncoded();
            privKeyEncoded = privKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.generateKeyPair();
    }

    public void regenerateKeys() {
        try {
            keyPair = generateKeyPair();
            pubKey = keyPair.getPublic();
            privKey = keyPair.getPrivate();
            pubKeyEncoded = pubKey.getEncoded();
            privKeyEncoded = privKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}