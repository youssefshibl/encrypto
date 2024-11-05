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

         // Path path = Paths.get(System.getProperty("user.dir"), "config.properties");
         Path jarDir = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

         // Construct the path to the config.properties file beside the JAR
         Path configPath = jarDir.resolve("config.properties");

         // Load properties from the config file
         loadproperties = new Loadproperties(configPath.toString());
         // arguments = new Arguments(args);
         // new CheckNetwork(arguments);
         // if (arguments.isServer()) {
         //    NioSslServer server = new NioSslServer(arguments);
         //    server.start();
         // } else {
         //    NioSslClient client = new NioSslClient(arguments);
         //    client.start();
         // }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}