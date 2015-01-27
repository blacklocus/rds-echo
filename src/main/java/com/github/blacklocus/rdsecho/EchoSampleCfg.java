package com.github.blacklocus.rdsecho;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class EchoSampleCfg implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(EchoSampleCfg.class);

    @Override
    public Boolean call() throws Exception {

        URL sampleProps = Resources.getResource(EchoConst.CONFIGURATION_PROPERTIES + ".sample");
        Path destination = Paths.get(EchoConst.CONFIGURATION_PROPERTIES);
        if (destination.toFile().exists()) {
            LOG.info("rdsecho.properties already exists at {}. Will not overwrite.", destination.toAbsolutePath());
            return false;
        }
        try (InputStream samplePropsInputStream = sampleProps.openStream()) {
            Files.copy(samplePropsInputStream, destination);
        }

        LOG.info("Sample configuration written to {}. While some property values are optional, it is strongly " +
                "recommended that all be populated.", destination.toAbsolutePath());

        return false;
    }
}
