package com.blacklocus.rds;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.blacklocus.rds.utl.EchoUtil;
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
            if (!EchoUtil.prompt("reboot", "Are you sure you would like to reboot instance %s? Input %s to confirm.")) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        LOG.info("Rebooting instance {}", dbInstanceId);
        rds.rebootDBInstance(new RebootDBInstanceRequest());

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoReboot().call();
    }
}
