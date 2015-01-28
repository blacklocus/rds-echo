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

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.github.blacklocus.rdsecho.utl.EchoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoRetire extends AbstractEchoIntermediateStage {

    private static final Logger LOG = LoggerFactory.getLogger(EchoRetire.class);

    public EchoRetire() {
        super(EchoConst.STAGE_PROMOTED, EchoConst.STAGE_RETIRED);
    }

    @Override
    boolean traverseStage(DBInstance instance) {

        String dbInstanceId = instance.getDBInstanceIdentifier();
        LOG.info("Propose to retire (destroy) instance {}", dbInstanceId);

        if (cfg.interactive()) {
            String format = "Are you sure you want to retire this instance? Input %s to confirm.";
            if (!EchoUtil.prompt(dbInstanceId, format, dbInstanceId)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        LOG.info("Retiring instance {}", dbInstanceId);
        DeleteDBInstanceRequest request = new DeleteDBInstanceRequest()
                .withDBInstanceIdentifier(dbInstanceId)
                .withSkipFinalSnapshot(cfg.retireSkipFinalSnapshot().orNull())
                .withFinalDBSnapshotIdentifier(cfg.retireFinalDbSnapshotIdentifier().orNull());
        rds.deleteDBInstance(request);
        LOG.info("So long {}", dbInstanceId);

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoRetire().call();
    }
}
