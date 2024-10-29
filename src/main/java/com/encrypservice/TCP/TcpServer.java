package com.encrypservice.TCP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

import com.encrypservice.Arguments;
import com.encrypservice.EncryptMessage;
import com.encrypservice.GenerateKeys;
import com.encrypservice.Loadproperties;
import com.encrypservice.SymmetricEncryption;

public class TcpServer {

    Arguments arguments;
    Loadproperties loadproperties;
    SocketChannel clientChannel;
    byte[] publicKeyBytes;
    EncryptMessage encryptMessage;
    SymmetricEncryption symmetricEncryption;

    public TcpServer(Arguments arguments, Loadproperties loadproperties) {
        this.arguments = arguments;
        this.loadproperties = loadproperties;
    }

    public void start(String[] args) {
        try {
            // Get public key from client
            GetPublicKeyFromClient();

            int PORT = Integer.parseInt(arguments.ServerPort);
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + PORT);

            while (true) {

                if (selector.select(1000) == 0)
                    continue;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    try {
                        if (key.isAcceptable()) {

                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            SocketChannel clientChannel = serverChannel.accept();
                            clientChannel.configureBlocking(false);
                            System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());
                            clientChannel.register(selector, SelectionKey.OP_READ);
                        }

                        if (key.isReadable()) {

                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead == -1) {
                                clientChannel.close();
                                System.out.println("Connection closed by client");
                            } else {
                                buffer.flip();
                                byte[] messageBytes = new byte[bytesRead];
                                buffer.get(messageBytes);
                                // print length of message
                                System.out.println("Received message with length: " + messageBytes.length);
                                System.out.println("Received message from client: " + new String(messageBytes, "UTF-8"));
                                byte[] encryptedMessage = encryptMessage.encrypt(messageBytes);
                                System.out.println("Encrypted message: " + new String(encryptedMessage,"UTF-8"));
                                System.out.println("Encrypted message length: " + encryptedMessage.length);
                                // send the encrypted message to the client
                                ByteBuffer encryptedMessageBuffer = ByteBuffer.wrap(encryptedMessage);
                                this.clientChannel.write(encryptedMessageBuffer);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(
                                "Error handling client connection: " + e.getMessage() + "with ip " + key.channel());
                        key.cancel();
                        key.channel().close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void GetPublicKeyFromClient() {
        try {
            // Connect to client
            clientChannel = SocketChannel.open();
            clientChannel.connect(new InetSocketAddress(arguments.ClientHost, Integer.parseInt(arguments.ClientPort)));

            // Send command to get public key
            ByteBuffer commandBuffer = ByteBuffer.allocate(256);
            commandBuffer.put("cm0:getRSAKey".getBytes("UTF-8"));
            commandBuffer.flip();
            clientChannel.write(commandBuffer);

            // Read the public key response from the client
            ByteBuffer publicKeyBuffer = ByteBuffer.allocate(2048);
            int bytesRead = clientChannel.read(publicKeyBuffer);

            if (bytesRead == -1) {
                throw new IOException("Failed to read public key from client");
            }

            // Process the received public key bytes
            publicKeyBuffer.flip();
            publicKeyBytes = new byte[bytesRead];
            publicKeyBuffer.get(publicKeyBytes);
            // System.out.println("Received public key from client: " + new String(publicKeyBytes, "UTF-8"));
            byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyBytes);
            encryptMessage = new EncryptMessage(decodedPublicKey, null);
            System.out.println("Received public key from client");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void SendAESKeyToClient() {

        try {
            // symmetricEncryption = new SymmetricEncryption();
            // symmetricEncryption.generateKey();
            // byte[] aesKey = symmetricEncryption.secretKey.getEncoded();
            // // Send command to get AES key
            // ByteBuffer commandBuffer = ByteBuffer.allocate(256);
            // byte[] command = "cm0:setAESKey:".getBytes("UTF-8")
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
