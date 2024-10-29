package com.encrypservice;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * CheckNetwork
 */
public class CheckNetwork {

    public CheckNetwork(Arguments argument) {
        // check is server port not assigned to any other service
        String serverPort = argument.ServerPort;
        String clientHost = argument.ClientHost;
        if (isPortAvailable(Integer.parseInt(serverPort))) {
            System.out.println("Server Port is Available");
        } else {
            throw new IllegalArgumentException("Server Port is already in use or not available");
        }
        // check if client ip is reachable
        if (isReachable(clientHost)) {
            System.out.println("Client Host is Reachable");
        } else {
            throw new IllegalArgumentException("Client Host is not reachable or not available");
        }

    }

    public boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true; // Port is available
        } catch (IOException e) {
            return false; // Port is already in use
        }
    }

    public boolean isReachable(String host) {
        try {
            return java.net.InetAddress.getByName(host).isReachable(3000);
        } catch (IOException e) {
            return false;
        }
    }
}