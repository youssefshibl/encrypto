package com.encrypservice.socket;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Iterator;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.encrypservice.Arguments;

public class NioSslClient extends NioSslPeer {

    private SSLEngine engine;
    private SocketChannel socketChannel;
    private Arguments arguments;
    ServerSocketChannel serverUnEncryptedChannel;
    private Selector selector;

    public NioSslClient(Arguments arguments) throws Exception {
        this.arguments = arguments;
        SSLContext context = SSLContext.getInstance("TLSv1.2");

        // Load keystore and truststore from resources
        try (InputStream keyStoreStream = getClass().getClassLoader().getResourceAsStream("client.jks");
                InputStream trustStoreStream = getClass().getClassLoader().getResourceAsStream("trustedCerts.jks")) {

            if (keyStoreStream == null || trustStoreStream == null) {
                throw new FileNotFoundException("Keystore or truststore file not found in resources");
            }

            context.init(createKeyManagers(keyStoreStream, "storepass", "storepass"),
                    createTrustManagers(trustStoreStream, "storepass"), new SecureRandom());
        }

        engine = context.createSSLEngine(arguments.ClientHost, Integer.parseInt(arguments.ClientPort));
        engine.setUseClientMode(true);
        SSLSession session = engine.getSession();
        myAppData = ByteBuffer.allocate(1024);
        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(1024);
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    // Helper methods to create KeyManagers and TrustManagers from InputStream
    private KeyManager[] createKeyManagers(InputStream keyStoreStream, String keyStorePassword, String keyPassword)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(keyStoreStream, keyStorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }

    private TrustManager[] createTrustManagers(InputStream trustStoreStream, String trustStorePassword)
            throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(trustStoreStream, trustStorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    public void start() throws Exception {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(
                    new InetSocketAddress(this.arguments.ClientHost, Integer.parseInt(this.arguments.ClientPort)));
            while (!socketChannel.finishConnect()) {
                log.debug("Still connecting...");
            }
            engine.beginHandshake();
            doHandshake(socketChannel, engine);
            startUnEncryptedServer();
            StartReciveUnEncryptedServer();
        } catch (Exception e) {
            log.error("Error while starting client: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void write(String message) throws IOException {
        write(socketChannel, engine, message);
    }

    protected void write(SocketChannel socketChannel, SSLEngine engine, String message) throws IOException {

        // log.debug("About to write to the server...");

        myAppData.clear();
        myAppData.put(message.getBytes());
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the
            // remote peer.
            myNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
                case OK:
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        socketChannel.write(myNetData);
                    }
                    log.debug("Message sent to the server: " + message);
                    break;
                case BUFFER_OVERFLOW:
                    myNetData = enlargePacketBuffer(engine, myNetData);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException(
                            "Buffer underflow occured after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    closeConnection(socketChannel, engine);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

    }

    public void read() throws Exception {
        read(socketChannel, engine);
    }

    protected void read(SocketChannel socketChannel, SSLEngine engine) throws Exception {

        // log.debug("About to read from the server...");

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
                    switch (result.getStatus()) {
                        case OK:
                            peerAppData.flip();
                            log.debug("Server response: " + new String(peerAppData.array()));
                            exitReadLoop = true;
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                            break;
                        case BUFFER_UNDERFLOW:
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
                            closeConnection(socketChannel, engine);
                            return;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannel, engine);
                return;
            }
            Thread.sleep(waitToReadMillis);
        }
    }

    public void shutdown() throws IOException {
        log.debug("About to close connection with the server...");
        closeConnection(socketChannel, engine);
        executor.shutdown();
        log.debug("Goodbye!");
    }

    public void startUnEncryptedServer() {
        // make new channel server
        try {
            // start server
            selector = SelectorProvider.provider().openSelector();
            serverUnEncryptedChannel = ServerSocketChannel.open();
            serverUnEncryptedChannel.configureBlocking(false);
            serverUnEncryptedChannel.socket().bind(new InetSocketAddress(Integer.parseInt(arguments.ServerPort)));
            serverUnEncryptedChannel.register(selector, SelectionKey.OP_ACCEPT, "unednecrypted");
        } catch (IOException e) {
            log.error("Error while starting unencrypted server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void StartReciveUnEncryptedServer() {
        try {
            while (true) {
                selector.select(); // will block until a channel is ready for I/O
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    // get attachment
                    String attachment = (String) key.attachment();
                    // check if attachment is uneencrypted
                    if (attachment.equals("unednecrypted")) {
                        if (key.isAcceptable()) {
                            SocketChannel client = serverUnEncryptedChannel.accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ, "unednecrypted");
                        }
                        if (key.isReadable()) {
                            // check if client want close connection
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int numRead = -1;
                            numRead = client.read(buffer);
                            if (numRead == -1) {
                                client.close();
                                key.cancel();
                                continue;
                            }
                            byte[] data = new byte[numRead];
                            System.arraycopy(buffer.array(), 0, data, 0, numRead);
                            String result = new String(data);
                            log.debug("Unencrypted message from client: " + result);
                            write(result);
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
