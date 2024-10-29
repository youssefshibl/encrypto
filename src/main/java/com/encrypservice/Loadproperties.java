package com.encrypservice;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class Loadproperties {
    Properties properties;

    public Loadproperties(String fileName) throws IOException {
        System.out.println("Loading properties from file: " + fileName);
        properties = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IOException("Sorry, unable to find " + fileName);
            }
            properties.load(input);
        }

    }
}