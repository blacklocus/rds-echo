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
