package com.blacklocus.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.blacklocus.rds.utl.RdsEchoUtil;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class EchoModify implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(EchoModify.class);

    final AmazonRDS rds = new AmazonRDSClient();

    final EchoCfg cfg = new EchoCfg();
    final RdsEchoUtil echo = new RdsEchoUtil();

    @Override
    public Boolean call() throws Exception {

        // Validate state, make sure we're operating on what we expect to.
        
        LOG.info("Locating latest Echo managed instance tagged {}.", echo.getTagEchoManaged());
        Optional<DBInstance> instanceOpt = echo.lastEchoInstance();
        if (!instanceOpt.isPresent()) {
            LOG.error("  Unable to locate Echo-managed instance. Is there one? Aborting.", echo.getTagEchoManaged());
            return false;
        }

        DBInstance instance = instanceOpt.get();
        Optional<Tag> stageOpt = echo.instanceStage(instance.getDBInstanceIdentifier());
        String tagEchoStage = echo.getTagEchoStage();
        if (!stageOpt.isPresent()) {
            LOG.error("Unable to read Echo stage tag so cannot determine stage. To forcefully set the stage, edit " +
                            "the instance's tags to add {}={} and run this modify operation again. " +
                            "Cannot continue as it is so exiting.",
                    tagEchoStage, EchoConst.STAGE_NEW);
            return false;
        }
        String instanceStage = stageOpt.get().getValue();
        if (!EchoConst.STAGE_NEW.equals(instanceStage)) {
            LOG.error("Current Echo stage on instance is {}={} but needs to be {}={}. To forcefully set the stage, " +
                            "edit the instance's tags to set the required stage and run this modify operation again. " +
                            "Cannot continue as it is so exiting.",
                    tagEchoStage, instanceStage, tagEchoStage, EchoConst.STAGE_NEW);
        }

        // Looks like we found a good instance to modify.

        String dbInstanceId = instance.getDBInstanceIdentifier();
        LOG.info("  Located echo-managed instance with identifier {}", dbInstanceId);
        if (!"available".equals(instance.getDBInstanceStatus())) {
            LOG.error("  Instance does not have status 'available' (saw {}). Cannot modify so aborting.",
                    instance.getDBInstanceStatus());
            return false;
        }

        // Prepare request and build up informational message with conditional parts.

        StringWriter proposed = new StringWriter();
        PrintWriter printer = new PrintWriter(proposed);
        printer.format("Proposed db modifications...%n");

        ModifyDBInstanceRequest request = new ModifyDBInstanceRequest();
        Optional<String> dbParameterGroupNameOpt = cfg.modDbParameterGroupName();
        if (dbParameterGroupNameOpt.isPresent()) {
            request.withDBParameterGroupName(dbParameterGroupNameOpt.get());
            printer.format("  db param group name    : %s%n", dbParameterGroupNameOpt.get());
        }
        Optional<String[]> dbSecurityGroupsOpt = cfg.modDbSecurityGroups();
        if (dbSecurityGroupsOpt.isPresent()) {
            request.withDBSecurityGroups(dbSecurityGroupsOpt.get());
            printer.format("  db security groups     : %s%n", Arrays.asList(dbSecurityGroupsOpt.get()));
        }
        Optional<Integer> backupRetentionPeriodOpt = cfg.modBackupRetentionPeriod();
        if (backupRetentionPeriodOpt.isPresent()) {
            request.withBackupRetentionPeriod(backupRetentionPeriodOpt.get());
            printer.format("  backup retention period: %d%n", backupRetentionPeriodOpt.get());
        }
        boolean applyImmediately = cfg.modApplyImmediately();
        printer.format("  apply immediately      : %b%n", applyImmediately);
        request.withApplyImmediately(applyImmediately);

        LOG.info(proposed.toString());

        // Interactive user confirm

        if (cfg.interactive()) {
            String format = "Proceed to modify DB instance with these settings? Input %s to confirm.";
            if (!RdsEchoUtil.prompt(dbInstanceId, format, dbInstanceId)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        // Do the deed

        LOG.info("Modifying existing DB instance.");
        rds.modifyDBInstance(request);

        // TODO option to wait for modify and reboot
        LOG.info("Submitted instance modify request. The instance may need to be rebooted to receive the effect of " +
                "certain settings. See AWS RDS documentation for details:\n" +
                "http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Overview.DBInstance.html#Overview.DBInstance.Modifying");

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoModify().call();
    }
}
