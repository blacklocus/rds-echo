/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 BlackLocus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

public class EchoSampleProps implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(EchoSampleProps.class);

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
