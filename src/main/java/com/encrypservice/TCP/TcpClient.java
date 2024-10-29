package com.encrypservice.TCP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

import com.encrypservice.Arguments;
import com.encrypservice.EncryptMessage;
import com.encrypservice.GenerateKeys;
import com.encrypservice.Loadproperties;

public class TcpClient {

    Arguments arguments;
    Loadproperties loadproperties;
    GenerateKeys generateKeys;
    boolean KeyGenerated = false;
    EncryptMessage encryptMessage;
    SocketChannel clientChannel;
    

    public TcpClient(Arguments arguments, Loadproperties loadproperties) {
        this.arguments = arguments;
        this.loadproperties = loadproperties;
        ConnectedToClient();
    }

    public void start(String[] args) {
        try {
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
                            System.out.println("Bytes read: " + bytesRead);

                            if (bytesRead == -1) {
                                clientChannel.close();
                                System.out.println("Connection closed by client");
                            } else {
                                // check if it command
                                buffer.flip();
                                byte[] initialBytes = new byte[4];
                                buffer.get(initialBytes, 0, 4);
                                String initialMessage = new String(initialBytes);
                                if (initialMessage.equals("cm0:")) {
                                    System.out.println("Received a command from client");
                                    byte[] commandBytes = new byte[buffer.remaining()];
                                    buffer.get(commandBytes);
                                    String command = new String(commandBytes).trim();
                                    System.out.println("Received a command: " + command);
                                    byte[] response = HandleCommand(command);
                                    clientChannel.write(ByteBuffer.wrap(response));
                                    buffer.flip();
                                    buffer.clear();
                                } else {
                                    // get all the bytes
                                    buffer.position(0);
                                    byte[] messageBytes = new byte[buffer.remaining()];
                                    System.out.println("Bytes remaining: " + buffer.remaining());
                                    buffer.get(messageBytes);
                                    // print length of message
                                    System.out.println("Received message with length: " + messageBytes.length);
                                    System.out.println("Received message from client: " + new String(messageBytes, "UTF-8"));
                                    // decrypt the message
                                    byte[] decryptedMessage = encryptMessage.decrypt(messageBytes);
                                    System.out.println("Decrypted message: " + new String(decryptedMessage, "UTF-8"));
                                    System.out.println("Decrypted message length: " + decryptedMessage.length);
                                    this.clientChannel.write(ByteBuffer.wrap(decryptedMessage));

                                
                                }

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

    public byte[] HandleCommand(String command) {
        try {
            if (command.equals("getRSAKey")) {
                if (!KeyGenerated) {
                    generateKeys = new GenerateKeys();
                    encryptMessage = new EncryptMessage(generateKeys.pubKeyEncoded, generateKeys.privKeyEncoded);
                    KeyGenerated = true;
                    byte[] encodedPublicKey = Base64.getEncoder().encode(generateKeys.pubKeyEncoded);
                    System.out.println("Sending public key to client: " + new String(encodedPublicKey, "UTF-8"));
                    return encodedPublicKey;
                } else {
                    return Base64.getEncoder().encode(generateKeys.pubKeyEncoded);
                }
            } else {
                return "Invalid Command".getBytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error".getBytes();
        }
    }

    public void ConnectedToClient(){
        try {
            // Connect to client
            clientChannel = SocketChannel.open();
            clientChannel.connect(new InetSocketAddress(arguments.ClientHost, Integer.parseInt(arguments.ClientPort)));

            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
