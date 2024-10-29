package com.encrypservice;

/**
 * Arguments
 */
public class Arguments {

    public String Mode;
    public String ServerPort;
    public String ClientPort;
    public String ClientHost;

    public Arguments(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException("Invalid number of arguments passed, should be 4, <server/client> <serverPort> <clientHost> <clientPort>");
        }
        // First argument is the Mode [Server/Client]
        // Second argument is List of Host Numbers In Server/Client Mode
        // Third argument is List of Port Names In Server/Client Mode
        Mode = args[0];
        ServerPort = args[1];
        ClientHost = args[2];
        ClientPort = args[3];
        // make validations here
        if (Mode.toLowerCase().equals("server") || Mode.toLowerCase().equals("client")) {
            Mode = Mode.toLowerCase();
            System.out.println("Mode: " + Mode);
        } else {
            throw new IllegalArgumentException("Mode should be either Server or Client");
        
        }
        // Check if ServerPort is in range 1024-65535
        if (Integer.parseInt(ServerPort) >= 1024 && Integer.parseInt(ServerPort) <= 65535) {
            System.out.println("ServerPort: " + ServerPort);
        } else {
            throw new IllegalArgumentException("Port should be in range 1024-65535");
        }
        // check if host is ip format
        if (ClientHost.matches(
                "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]).){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
            System.out.println("Host: " + ClientHost);
        } else {
            throw new IllegalArgumentException("Host should be in IP format");
        }
        // check if port is in range 1024-65535
        if (Integer.parseInt(ClientPort) >= 1024 && Integer.parseInt(ClientPort) <= 65535) {
            System.out.println("ClientPort: " + ClientPort);
        } else {
            throw new IllegalArgumentException("Port should be in range 1024-65535");
        }
    }
    public boolean isServer() {
        return Mode.equals("server");
    }
    public boolean isClient() {
        return Mode.equals("client");
    }
    
}