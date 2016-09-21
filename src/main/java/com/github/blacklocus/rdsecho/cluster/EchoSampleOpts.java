package com.github.blacklocus.rdsecho.cluster;

import com.github.blacklocus.rdsecho.instance.EchoCfg;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

public class EchoSampleOpts implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(EchoSampleOpts.class);

    @Override
    public Boolean call() throws Exception {

        CompositeConfiguration values = new CompositeConfiguration();

        try {
            PropertiesConfiguration existing = new PropertiesConfiguration();
            existing.setDelimiterParsingDisabled(true);
            existing.load(EchoConst.CONFIGURATION_PROPERTIES);
            values.addConfiguration(existing);
            LOG.info("Preferring values defined in {}", EchoConst.CONFIGURATION_PROPERTIES);
        } catch (ConfigurationException e) {
            LOG.debug("No {} found. Will not include values from any such file.", EchoConst.CONFIGURATION_PROPERTIES);
        }

        PropertiesConfiguration sample = new PropertiesConfiguration();
        sample.setDelimiterParsingDisabled(true);
        sample.load("rdsecho.properties.sample");

        values.addConfiguration(sample);

        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s);
        p.format("export RDS_ECHO_OPTS=\"%n");

        Field[] f = EchoCfg.class.getDeclaredFields();
        for (Field field : f) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers()) &&
                    field.getName().startsWith("PROP_")) {
                String propName = field.get(EchoCfg.class).toString();
                Object templateVal = values.getProperty(propName);
                p.format("    -D%s=%s %n", propName, templateVal);
            }
        }

        p.format("\"%n");

        System.out.println(s);

        LOG.info("Please review the output for any necessary properties that were not been defined.");

        return false;
    }
}
