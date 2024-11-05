package com.encrypservice;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.encrypservice.socket.NioSslClient;
import com.encrypservice.socket.NioSslServer;

public class Main {

   static Loadproperties loadproperties;
   static Arguments arguments;

   public static void main(String[] args) {

      try {

         String propertiesFileName = "config.properties";

         // Load properties from the config file
         loadproperties = new Loadproperties("config.properties");
         arguments = new Arguments(args);
         new CheckNetwork(arguments);
         if (arguments.isServer()) {
            NioSslServer server = new NioSslServer(arguments);
            server.start();
         } else {
            NioSslClient client = new NioSslClient(arguments);
            client.start();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}
