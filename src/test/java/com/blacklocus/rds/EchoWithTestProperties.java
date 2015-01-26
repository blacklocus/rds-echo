package com.blacklocus.rds;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

public class EchoWithTestProperties {

    static void bootstrapFromPropertiesFile() throws IOException {
        // bootstrap values into system properties
        Properties properties = new Properties();
        try (Reader reader = new FileReader("rdsecho.properties")) {
            properties.load(reader);
        }

        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            System.setProperty(e.getKey().toString(), e.getValue().toString());
        }
    }

    public static class New {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            EchoNew.main(args);
        }
    }

    public static class Modify {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            EchoModify.main(args);
        }
    }

    public static class Reboot {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            EchoReboot.main(args);
        }
    }

    public static class Promote {
        public static void main(String[] args) throws Exception {
            bootstrapFromPropertiesFile();
            EchoPromote.main(args);
        }
    }

}
