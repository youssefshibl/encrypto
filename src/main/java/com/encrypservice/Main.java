package com.encrypservice;

import com.encrypservice.Socket.SSLClient;
import com.encrypservice.Socket.Server;
import com.encrypservice.TCP.TcpClient;
import com.encrypservice.TCP.TcpServer;

public class Main {

   static Loadproperties loadproperties;
   static GenerateKeys generateKeys;
   static Arguments arguments;
   static EncryptMessage encryptMessage;
   static TcpServer tcpServer;
   static TcpClient tcpClient;
   static Server Server;
   static SSLClient sslClient;

   public static void main(String[] args) {

      try {

         // load properties & arguments & check network
         loadproperties = new Loadproperties("config.properties");
         arguments = new Arguments(args);
         new CheckNetwork(arguments);
         // start server or client
         if (arguments.isServer()) {
            Server = new Server(arguments, loadproperties);
            Server.start(new String[] {});
         } else {
            SSLClient.main(new String[] {});
         }

      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}