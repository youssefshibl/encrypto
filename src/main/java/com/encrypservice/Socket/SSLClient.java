package com.encrypservice.Socket;

import javax.net.ssl.*;

import java.io.*;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SSLClient {

    private static final String HOST = "localhost";
    private static final int PORT = 8441;
    private static final String TRUSTSTORE = "truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "changeit";

    public static void main(String[] args) {
        try {
            SSLContext sslContext = createSSLContext();
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(HOST, PORT)) {
                socket.startHandshake(); // Initiates the SSL handshake

                // Set up input and output streams
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a message to the server
                String message = "Hello, secure server!";
                out.println(message);
                System.out.println("Sent: " + message);

                // Read and print the server's response
                String response = in.readLine();
                System.out.println("Received: " + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SSLContext createSSLContext() throws Exception {
        // Load the TrustStore
        System.out.println("Creating SSL context");

        KeyStore trustStore = KeyStore.getInstance("JKS");

        try (InputStream trustStoreStream = ClassLoader.getSystemResourceAsStream(TRUSTSTORE)) {
            if (trustStoreStream == null) {
                throw new FileNotFoundException("TrustStore not found: " + TRUSTSTORE);
            }
            trustStore.load(trustStoreStream, TRUSTSTORE_PASSWORD.toCharArray());
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            System.err.println("Error loading TrustStore: " + e.getMessage());
            throw e; // Re-throw the exception after logging
        }

        // Initialize TrustManagerFactory with the TrustStore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Create the SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null); // Use TrustManagers and no KeyManagers

        return sslContext;
    }

}
