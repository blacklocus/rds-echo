package com.blacklocus.rds;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

public class DbEchoWithTestProperties {

    static void bootstrapFromPropertiesFile() throws IOException {
        // bootstrap values into system properties
        Properties properties = new Properties();
        try (Reader reader = new FileReader("dbecho.properties")) {
            properties.load(reader);
        }

        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            System.setProperty(e.getKey().toString(), e.getValue().toString());
        }
    }

    public static class New {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            DbEchoNew.main(args);
        }
    }

    public static class Promote {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            DbEchoPromote.main(args);
        }
    }

}
