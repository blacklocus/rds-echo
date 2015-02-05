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

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.utl.EchoUtil;
import com.github.blacklocus.rdsecho.utl.RdsFind;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

abstract class AbstractEchoIntermediateStage implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEchoIntermediateStage.class);

    final String requisiteStage;
    final String resultantStage;

    final AmazonRDS rds = new AmazonRDSClient();

    final EchoCfg cfg = EchoCfg.getInstance();
    final EchoUtil echo = new EchoUtil();

    public AbstractEchoIntermediateStage(String requisiteStage, String resultantStage) {
        this.requisiteStage = requisiteStage;
        this.resultantStage = resultantStage;
    }

    @Override
    public Boolean call() throws Exception {

        // Validate state, make sure we're operating on what we expect to.

        String tagEchoManaged = echo.getTagEchoManaged();
        String tagEchoStage = echo.getTagEchoStage();

        LOG.info("Locating latest Echo managed instance (tagged with {}=true) in stage '{}' (tagged with {}={}).",
                tagEchoManaged, requisiteStage, tagEchoStage, requisiteStage);
        Optional<DBInstance> instanceOpt = echo.lastEchoInstance();
        if (!instanceOpt.isPresent()) {
            LOG.error("  Unable to locate Echo-managed instance. Is there one? Aborting.", tagEchoManaged);
            return false;
        }

        DBInstance instance = instanceOpt.get();
        Optional<Tag> stageOpt = echo.instanceStage(instance.getDBInstanceIdentifier());
        if (!stageOpt.isPresent()) {
            LOG.error("Unable to read Echo stage tag so cannot determine stage. To forcefully set the stage, edit " +
                            "the instance's tags to add {}={} and run this modify operation again. " +
                            "Cannot continue as it is so exiting.",
                    tagEchoStage, requisiteStage);
            return false;
        }
        String instanceStage = stageOpt.get().getValue();
        if (!requisiteStage.equals(instanceStage)) {
            LOG.error("Current Echo stage on instance is {}={} but needs to be {}={}. To forcefully set the stage, " +
                            "edit the instance's tags to set the required stage and run this modify operation again. " +
                            "Cannot continue as it is so exiting.",
                    tagEchoStage, instanceStage, tagEchoStage, requisiteStage);
            return false;
        }

        // Looks like we found a good echo instance, but is it available to us.

        String dbInstanceId = instance.getDBInstanceIdentifier();
        LOG.info("  Located echo-managed instance with identifier {}", dbInstanceId);
        if (!"available".equals(instance.getDBInstanceStatus())) {
            LOG.error("  Instance does not have status 'available' (saw {}) so aborting.",
                    instance.getDBInstanceStatus());
            return false;
        }

        // Do the part special to traversing the this stage

        if (traverseStage(instance)) {

            // Advance. This replaces, same-named tags.
            rds.addTagsToResource(new AddTagsToResourceRequest()
                    .withResourceName(RdsFind.instanceArn(cfg.region(), cfg.accountNumber(), instance.getDBInstanceIdentifier()))
                    .withTags(new Tag().withKey(tagEchoStage).withValue(resultantStage)));

            return true;

        } else {
            return false;
        }
    }

    /**
     * @param instance which guaranteed to be non-null, available, and on the requisite stage.
     */
    abstract boolean traverseStage(DBInstance instance);
}
