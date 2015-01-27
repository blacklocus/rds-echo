package com.github.blacklocus.rdsecho;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.github.blacklocus.rdsecho.utl.EchoUtil;
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

        LOG.info("Rebooting instance {}", dbInstanceId);
        rds.rebootDBInstance(new RebootDBInstanceRequest()
                .withDBInstanceIdentifier(dbInstanceId));

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoReboot().call();
    }
}
