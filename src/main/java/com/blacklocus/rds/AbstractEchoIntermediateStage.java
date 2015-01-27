package com.blacklocus.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Tag;
import com.blacklocus.rds.utl.EchoUtil;
import com.blacklocus.rds.utl.RdsFind;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

abstract class AbstractEchoIntermediateStage implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEchoIntermediateStage.class);

    final String requisiteStage;
    final String resultantStage;

    final AmazonRDS rds = new AmazonRDSClient();

    final EchoCfg cfg = new EchoCfg();
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
