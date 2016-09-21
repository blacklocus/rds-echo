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
package com.github.blacklocus.rdsecho.cluster;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.github.blacklocus.rdsecho.cluster.utl.EchoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoReboot extends AbstractEchoIntermediateStage {

    private static final Logger LOG = LoggerFactory.getLogger(EchoReboot.class);

    public EchoReboot() {
        super(EchoConst.STAGE_MODIFIED, EchoConst.STAGE_REBOOTED);
    }

    @Override
    boolean traverseStage(DBInstance instance) {

        String dbInstanceId = instance.getDBInstanceIdentifier();
        if (cfg.interactive()) {
            if (!EchoUtil.prompt(dbInstanceId, "Are you sure you would like to reboot the instance? Input %s to confirm.",
                    dbInstanceId)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        LOG.info("[{}] Rebooting instance {}", getCommand(), dbInstanceId);
        rds.rebootDBInstance(new RebootDBInstanceRequest()
                .withDBInstanceIdentifier(dbInstanceId));

        return true;
    }

    @Override
    String getCommand() {
        return EchoConst.COMMAND_REBOOT;
    }

    public static void main(String[] args) throws Exception {
        new EchoReboot().call();
    }
}
