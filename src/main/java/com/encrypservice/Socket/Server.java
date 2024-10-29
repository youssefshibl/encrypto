package com.encrypservice.Socket;

import java.io.FileNotFoundException;

import javax.net.ssl.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Iterator;

import com.encrypservice.Arguments;
import com.encrypservice.Loadproperties;

public class Server {
    private static final int PORT = 8443;
    private static final String KEYSTORE = "keystore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";
    Arguments arguments;
    Loadproperties loadproperties;

    public Server(Arguments arguments, Loadproperties loadproperties) {
        this.arguments = arguments;
        this.loadproperties = loadproperties;
    }

    public void test() {
        try {
            System.out.println("Server test");
            SSLContext sslContext = createSSLContext();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(String[] args) throws Exception {
        SSLContext sslContext = createSSLContext();

        // Open non-blocking ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new java.net.InetSocketAddress(Integer.parseInt(arguments.ServerPort)));

        // Open Selector
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("SSL Server started on port " + PORT);

        while (true) {
            selector.select(); // Blocking until events are ready
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    handleAccept(key, sslContext, selector);
                } else if (key.isReadable()) {
                    handleRead(key);
                } else if (key.isWritable()) {
                    handleWrite(key);
                }
            }
        }
    }

    private SSLContext createSSLContext() throws Exception {
        System.out.println("Creating SSL context");
        // Load the KeyStore from resources
        KeyStore keyStore = KeyStore.getInstance("JKS");

        try (InputStream keyStoreStream = ClassLoader.getSystemResourceAsStream(KEYSTORE)) {
            if (keyStoreStream == null) {
                throw new FileNotFoundException("KeyStore not found: " + KEYSTORE);
            }
            keyStore.load(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            System.err.println("Error loading KeyStore: " + e.getMessage());
            throw e; // Re-throw the exception after logging
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext;
    }

    private void handleAccept(SelectionKey key, SSLContext sslContext, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // Create an SSLEngine for each client
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.beginHandshake();

        SSLSession sslSession = sslEngine.getSession();
        ByteBuffer appBuffer = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
        ByteBuffer netBuffer = ByteBuffer.allocate(sslSession.getPacketBufferSize());

        clientChannel.register(selector, SelectionKey.OP_READ,
                new SSLHandler(clientChannel, sslEngine, appBuffer, netBuffer));
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SSLHandler handler = (SSLHandler) key.attachment();
        handler.doHandshake(); // Handle SSL handshake if required
        handler.readData(); // Read and decrypt data
    }

    private static void handleWrite(SelectionKey key) throws IOException {
        SSLHandler handler = (SSLHandler) key.attachment();
        handler.writeData(); // Write encrypted data to client
    }
}
