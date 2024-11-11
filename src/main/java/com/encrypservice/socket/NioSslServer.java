package com.encrypservice.socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class NioSslServer extends NioSslPeer {

    private boolean active;
    private SSLContext context;
    private Selector selector;
    private Arguments arguments;
    private SocketChannel unencryptedServerSocketChannel;

    public NioSslServer(Arguments args) throws Exception {
        this.arguments = args;
        // File jarDir;
        // try {
        //     jarDir = new File(NioSslServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        //     System.out.println("JAR file path: " + jarDir.getAbsolutePath()); 
        // } catch (URISyntaxException e) {
        //     throw new IOException("Error determining the JAR directory.", e);
        // }
        Path executableDir = Paths.get(System.getProperty("user.dir"));
        // File keyStoreFile = new File(jarDir, "server.jks");
        // File trustStoreFile = new File(jarDir, "trustedCerts.jks");
        File keyStoreFile = new File(executableDir.toFile(), "server.jks");
        File trustStoreFile = new File(executableDir.toFile(), "trustedCerts.jks");

        context = SSLContext.getInstance("TLSv1.2");
        try (InputStream keyStoreStream = new FileInputStream(keyStoreFile);
             InputStream trustStoreStream = new FileInputStream(trustStoreFile)) {

            if (!keyStoreFile.exists() || !trustStoreFile.exists()) {
                throw new IOException("Keystore or truststore file not found beside the JAR file.");
            }

            // Initialize the SSLContext with key and trust managers
            context.init(
                createKeyManagers(keyStoreStream, "storepass", "storepass"),
                createTrustManagers(trustStoreStream, "storepass"),
                new SecureRandom()
            );
        }catch (IOException e) {
            throw new IOException("Error loading keystore or truststore from external file", e);
        }
        SSLSession dummySession = context.createSSLEngine().getSession();
        myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();
        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(Integer.parseInt(arguments.ServerPort)));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        active = true;

    }

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
            startUnEncryptedconnection();
            log.debug("Initialized and waiting for new connections...");

            while (isActive()) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read((SocketChannel) key.channel(), (SSLEngine) key.attachment());
                    }
                }
            }
            log.debug("Goodbye!");
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
            stop();
        }

    }

    public void stop() {
        log.debug("Will now close server...");
        active = false;
        executor.shutdown();
        selector.wakeup();
    }

    private void accept(SelectionKey key) throws Exception {

        log.debug("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            log.debug("Connection closed due to handshake failure.");
        }
    }

    protected void read(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        // log.debug("About to read from a client...");
        peerNetData.clear();
        int bytesRead = socketChannel.read(peerNetData);
        if (bytesRead > 0) {
            peerNetData.flip();
            while (peerNetData.hasRemaining()) {
                peerAppData.clear();
                SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
                switch (result.getStatus()) {
                    case OK:
                        peerAppData.flip();
                        String message = new String(peerAppData.array(), peerAppData.position(), peerAppData.limit() - peerAppData.position());
                        log.debug("Incoming message: " + message);
                        // check if unecrypted connection is active
                        try {
                            if (unencryptedServerSocketChannel.isOpen()) {
                                // write to unencrypted server
                                unencryptedServerSocketChannel.write(peerAppData);
                            } else {
                                log.error("Unencrypted connection is not active. Will try to reconnect...");
                                startUnEncryptedconnection();
                            }
                        } catch (IOException e) {
                            log.error("Error while writing to unencrypted server: " + e.getMessage());
                            e.printStackTrace();
                            startUnEncryptedconnection();
                        }

                        break;
                    case BUFFER_OVERFLOW:
                        peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = handleBufferUnderflow(engine, peerNetData);
                        break;
                    case CLOSED:
                        log.debug("Client wants to close connection...");
                        closeConnection(socketChannel, engine);
                        log.debug("Goodbye client!");
                        return;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

            // write(socketChannel, engine, "Hello! I am your server!");

        } else if (bytesRead < 0) {
            log.error("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(socketChannel, engine);
            log.debug("Goodbye client!");
        }
    }

    protected void write(SocketChannel socketChannel, SSLEngine engine, String message) throws IOException {

        // log.debug("About to write to a client...");

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
                    log.debug("Message sent to the client: " + message);
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

    private boolean isActive() {
        return active;
    }

    private void startUnEncryptedconnection() {
        try {
            // connect as client
            unencryptedServerSocketChannel = SocketChannel.open();
            unencryptedServerSocketChannel.connect(
                    new InetSocketAddress(this.arguments.ClientHost, Integer.parseInt(this.arguments.ClientPort)));
            while (!unencryptedServerSocketChannel.finishConnect()) {
                log.debug("Still connecting to unencrypted server...");
            }
            log.debug("Connected to unencrypted server!");
        } catch (Exception e) {
            log.error("Could not connect to unencrypted server: " + e.getMessage());
            e.printStackTrace();
            // System.exit(1);
        }
    }

}
