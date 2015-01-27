package com.github.blacklocus.rdsecho;


import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.utl.EchoUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class EchoNew implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(EchoNew.class);

    final AmazonRDS rds = new AmazonRDSClient();

    final EchoCfg cfg = new EchoCfg();
    final EchoUtil echo = new EchoUtil();

    @Override
    public Boolean call() throws Exception {

        // Do some sanity checks to make sure we aren't generating a bunch of trouble in RDS

        String tagEchoManaged = echo.getTagEchoManaged();

        LOG.info("Checking to see if current echo-created instance (tagged {}) was created less than 24 hours ago. " +
                "If so this operation will not continue.", tagEchoManaged);
        Optional<DBInstance> newestInstanceOpt = echo.lastEchoInstance();
        if (newestInstanceOpt.isPresent()) {

            if (new DateTime(newestInstanceOpt.get().getInstanceCreateTime()).plusHours(24).isAfter(DateTime.now())) {
                LOG.info("  Last echo-created RDS instance {} was created less than 24 hours ago. Aborting.",
                        tagEchoManaged);
                return false;

            } else {
                LOG.info("  Last echo-created RDS instance {} was created more than 24 hours ago. Proceeding.",
                        tagEchoManaged);
            }

        } else {
            LOG.info("  No prior echo-created instance found with tag {}. Proceeding.", tagEchoManaged);
        }

        // Locate a suitable snapshot to be the basis of the new instance

        LOG.info("Locating latest snapshot from {}", cfg.snapshotDbInstanceIdentifier());
        Optional<DBSnapshot> dbSnapshotOpt = echo.latestSnapshot();
        if (dbSnapshotOpt.isPresent()) {
            DBSnapshot snapshot = dbSnapshotOpt.get();
            LOG.info("  Located snapshot {} completed on {}", snapshot.getDBSnapshotIdentifier(),
                    new DateTime(snapshot.getSnapshotCreateTime()).toDateTimeISO().toString());

        } else {
            LOG.info("  Could not locate a suitable snapshot. Cannot continue.");
            return false;
        }

        // Info summary

        String dbSnapshotIdentifier = dbSnapshotOpt.get().getDBSnapshotIdentifier();
        String newDbInstanceIdentifier = cfg.name() + '-' + DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd");
        LOG.info("Proposed new db instance...\n" +
                        "  engine           : {}\n" +
                        "  license model    : {}\n" +
                        "  db instance class: {}\n" +
                        "  multi az         : {}\n" +
                        "  storage type     : {}\n" +
                        "  iops             : {}\n" +
                        "  db snapshot id   : {}\n" +
                        "  db instance id   : {}\n" +
                        "  port             : {}\n" +
                        "  option group name: {}\n" +
                        "  auto minor ver up: {}",
                cfg.newEngine(),
                cfg.newLicenseModel(),
                cfg.newDbInstanceClass(),
                cfg.newMultiAz(),
                cfg.newStorageType(),
                cfg.newIops(),
                dbSnapshotIdentifier,
                newDbInstanceIdentifier,
                cfg.newPort(),
                cfg.newOptionGroupName(),
                cfg.newAutoMinorVersionUpgrade());

        // Interactive user confirmation

        if (cfg.interactive()) {
            String format = "Proceed to create a new DB instance from this snapshot? Input %s to confirm.";
            if (!EchoUtil.prompt(newDbInstanceIdentifier, format, newDbInstanceIdentifier)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        // Create the new database

        LOG.info("Creating new DB instance. Hold on to your butts.");
        RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest()
                .withEngine(cfg.newEngine())
                .withLicenseModel(cfg.newLicenseModel())
                .withDBInstanceClass(cfg.newDbInstanceClass())
                .withMultiAZ(cfg.newMultiAz())
                .withStorageType(cfg.newStorageType())
                .withIops(cfg.newIops())
                .withDBSnapshotIdentifier(dbSnapshotIdentifier)
                .withDBInstanceIdentifier(newDbInstanceIdentifier)
                .withPort(cfg.newPort())
                .withOptionGroupName(cfg.newOptionGroupName())
                .withAutoMinorVersionUpgrade(cfg.newAutoMinorVersionUpgrade())
                .withTags(
                        new Tag().withKey(echo.getTagEchoManaged()).withValue("true"),
                        new Tag().withKey(echo.getTagEchoStage()).withValue(EchoConst.STAGE_NEW)
                );
        rds.restoreDBInstanceFromDBSnapshot(request);

        LOG.info("Created new DB instance. \n" +
                        "  https://console.aws.amazon.com/rds/home?region={}#dbinstance:id={}\n" +
                        "Additional preparation of the instance will continue once the instance becomes available.",
                cfg.region(), newDbInstanceIdentifier);

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoNew().call();
    }
}
