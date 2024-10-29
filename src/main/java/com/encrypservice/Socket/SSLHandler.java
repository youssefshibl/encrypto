package com.encrypservice.Socket;

import javax.net.ssl.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SSLHandler {
    private final SocketChannel channel;
    private final SSLEngine sslEngine;
    private final ByteBuffer appData;
    private final ByteBuffer netData;
    private  ByteBuffer peerAppData;
    private  ByteBuffer peerNetData;
    private boolean handshaking = true;

    public SSLHandler(SocketChannel channel, SSLEngine sslEngine, ByteBuffer appData, ByteBuffer netData) {
        this.channel = channel;
        this.sslEngine = sslEngine;
        this.appData = appData;
        this.netData = netData;
        SSLSession session = sslEngine.getSession();
        this.peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        this.peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    public void doHandshake() throws IOException {
        if (!handshaking) return;

        SSLEngineResult result;
        SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
               handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    if (channel.read(peerNetData) < 0) {
                        sslEngine.closeInbound();
                        return;
                    }
                    peerNetData.flip();
                    result = sslEngine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();

                    switch (result.getStatus()) {
                        case OK:
                            handshakeStatus = result.getHandshakeStatus();
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = enlargeBuffer(peerAppData, sslEngine.getSession().getApplicationBufferSize());
                            break;
                        case BUFFER_UNDERFLOW:
                            peerNetData = enlargeBuffer(peerNetData, sslEngine.getSession().getPacketBufferSize());
                            break;
                        case CLOSED:
                            sslEngine.closeOutbound();
                            return;
                        default:
                            throw new IOException("Invalid handshake status: " + result.getStatus());
                    }
                    break;

                case NEED_WRAP:
                    netData.clear();
                    result = sslEngine.wrap(appData, netData);
                    netData.flip();

                    while (netData.hasRemaining()) {
                        channel.write(netData);
                    }

                    handshakeStatus = result.getHandshakeStatus();
                    break;

                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;

                default:
                    throw new IllegalStateException("Invalid handshake status: " + handshakeStatus);
            }
        }
        handshaking = false;
    }

    public void readData() throws IOException {
        if (handshaking) {
            doHandshake();
            return;
        }

        peerNetData.clear();
        if (channel.read(peerNetData) < 0) {
            sslEngine.closeInbound();
            return;
        }

        peerNetData.flip();
        SSLEngineResult result = sslEngine.unwrap(peerNetData, appData);
        peerNetData.compact();

        switch (result.getStatus()) {
            case OK:
                appData.flip();
                System.out.println("Received: " + new String(appData.array(), appData.position(), appData.limit()));
                appData.clear();
                break;
            case CLOSED:
                sslEngine.closeOutbound();
                break;
            default:
                throw new IOException("SSL read error: " + result.getStatus());
        }
    }

    public void writeData() throws IOException {
        if (handshaking) {
            doHandshake();
            return;
        }

        appData.flip();
        SSLEngineResult result = sslEngine.wrap(appData, netData);
        appData.compact();

        if (result.getStatus() == SSLEngineResult.Status.OK) {
            netData.flip();
            while (netData.hasRemaining()) {
                channel.write(netData);
            }
            netData.compact();
        } else if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            sslEngine.closeOutbound();
        }
    }

    private ByteBuffer enlargeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
