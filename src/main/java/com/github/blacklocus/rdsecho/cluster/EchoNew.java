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


import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.RestoreDBClusterFromSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.cluster.utl.EchoUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_NEW;

public class EchoNew implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(EchoNew.class);

    final AmazonRDS rds = new AmazonRDSClient();

    final EchoCfg cfg = EchoCfg.getInstance();
    final EchoUtil echo = new EchoUtil();

    @Override
    public Boolean call() throws Exception {

        // Do some sanity checks to make sure we aren't generating a bunch of trouble in RDS

        String tagEchoManaged = echo.getTagEchoManaged();

        LOG.info("[{}] Checking to see if current echo-created instance (tagged {}) was created less than 24 hours ago. " +
                "If so this operation will not continue.", COMMAND_NEW, tagEchoManaged);

        Optional<DBInstance> newestInstanceOpt = echo.lastEchoInstance();
        if (newestInstanceOpt.isPresent()) {

            if (new DateTime(newestInstanceOpt.get().getInstanceCreateTime()).plusHours(24).isAfter(DateTime.now())) {
                LOG.info("[{}] Last echo-created RDS instance {} was created less than 24 hours ago. Aborting.",
                        COMMAND_NEW, tagEchoManaged);
                return false;

            } else {
                LOG.info("[{}] Last echo-created RDS instance {} was created more than 24 hours ago. Proceeding.",
                        COMMAND_NEW, tagEchoManaged);
            }

        } else {
            LOG.info("[{}] No prior echo-created instance found with tag {}. Proceeding.", COMMAND_NEW, tagEchoManaged);
        }

        // Locate a suitable snapshot to be the basis of the new instance

        LOG.info("[{}] Locating latest snapshot from {}", COMMAND_NEW, cfg.snapshotClusterIdentifier());
        Optional<DBClusterSnapshot> dbSnapshotOpt = echo.latestSnapshot(cfg.snapshotClusterIdentifier());
        if (dbSnapshotOpt.isPresent()) {
            DBClusterSnapshot snapshot = dbSnapshotOpt.get();
            LOG.info("[{}] Located snapshot {} completed on {}", COMMAND_NEW, snapshot.getDBClusterSnapshotIdentifier(),
                    new DateTime(snapshot.getSnapshotCreateTime()).toDateTimeISO().toString());

        } else {
            LOG.info("[{}] Could not locate a suitable snapshot. Cannot continue.", COMMAND_NEW);
            return false;
        }

        // Info summary

        String clusterSnapshotIdentifier = dbSnapshotOpt.get().getDBClusterSnapshotIdentifier();
        String newInstanceIdentifier = cfg.name() + '-' + DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd");
        String newClusterIdentifier = newInstanceIdentifier + "-cluster";

        // Prepare request and build up informational message with conditional parts.

        StringWriter proposed = new StringWriter();
        PrintWriter printer = new PrintWriter(proposed);
        printer.format("[%s] Proposed new cluster and instance...%n", COMMAND_NEW);

        RestoreDBClusterFromSnapshotRequest auroraRequest = new RestoreDBClusterFromSnapshotRequest();

        // Required cluster settings

        auroraRequest.withDBClusterIdentifier(newClusterIdentifier);
        auroraRequest.withSnapshotIdentifier(clusterSnapshotIdentifier);
        auroraRequest.withEngine(cfg.newEngine());
        auroraRequest.withDBSubnetGroupName(cfg.newSubnetGroupName());

        printer.format("Cluster settings:%n");
        printer.format("  snapshot id      : %s%n", clusterSnapshotIdentifier);
        printer.format("  new cluster id   : %s%n", newClusterIdentifier);
        printer.format("  engine           : %s%n", cfg.newEngine());
        printer.format("  subnet group     : %s%n", cfg.newSubnetGroupName());

        // Optional cluster settings

        Optional<Integer> portOpt = cfg.newPort();
        if(portOpt.isPresent()) {
            auroraRequest.withPort(portOpt.get());
            printer.format("  port             : %s%n", portOpt.get());
        }

        Optional<String[]> vpcSecurityGroups = cfg.newVpcSecurityGroups();
        if (vpcSecurityGroups.isPresent()) {
            auroraRequest.withVpcSecurityGroupIds(vpcSecurityGroups.get());
            printer.format("  security groups  : %s%n", Arrays.asList(vpcSecurityGroups.get()));
        }

        // Required instance settings

        CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest();

        createDBInstanceRequest.withDBClusterIdentifier(newClusterIdentifier);
        createDBInstanceRequest.withDBInstanceIdentifier(newInstanceIdentifier);
        createDBInstanceRequest.withEngine(cfg.newEngine());
        createDBInstanceRequest.withDBInstanceClass(cfg.newDbInstanceClass());

        printer.format("Instance settings:%n");

        printer.format("  cluster id       : %s%n", newClusterIdentifier);
        printer.format("  new instance id  : %s%n", newInstanceIdentifier);
        printer.format("  engine           : %s%n", cfg.newEngine());
        printer.format("  db instance class: %s%n", cfg.newDbInstanceClass());

        // Optional instance settings

        Optional<Boolean> multiAzOpt = cfg.newMultiAz();
        if (multiAzOpt.isPresent()) {
            createDBInstanceRequest.withMultiAZ(multiAzOpt.get());
            printer.format("  multi az         : %s%n", multiAzOpt.get());
        }

        Optional<String> availabilityZone = cfg.newAvailabilityZone();
        if (availabilityZone.isPresent()) {
            createDBInstanceRequest.withAvailabilityZone(availabilityZone.get());
            printer.format("  availability zone: %s%n", availabilityZone.get());
        }

        Optional<Boolean> autoMinorVersionOpt = cfg.newAutoMinorVersionUpgrade();
        if (autoMinorVersionOpt.isPresent()) {
            createDBInstanceRequest.withAutoMinorVersionUpgrade(autoMinorVersionOpt.get());
            printer.format("  auto minor ver up: %s%n", autoMinorVersionOpt.get());
        }

        List<Tag> tags = new ArrayList<>();

        // Managed instance tags
        tags.add(new Tag().withKey(echo.getTagEchoManaged()).withValue("true"));
        tags.add(new Tag().withKey(echo.getTagEchoStage()).withValue(EchoConst.STAGE_NEW));

        // User tags
        Optional<String[]> newTags = cfg.newTags();
        if (newTags.isPresent()) {
            tags.addAll(EchoUtil.parseTags(newTags.get()));
        }

        if (tags.size() > 0) {
            printer.format("  tags             : %s%n", Arrays.asList(tags));
        }

        createDBInstanceRequest.withTags(tags);

        LOG.info(proposed.toString());

        // Interactive user confirmation

        if (cfg.interactive()) {
            String format = "Proceed to create a new DB instance from this snapshot? Input %s to confirm.";
            if (!EchoUtil.prompt(newInstanceIdentifier, format, newInstanceIdentifier)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        // Create the new database

        LOG.info("[{}] Creating new DB cluster and instance. Hold on to your butts.", COMMAND_NEW);
        DBCluster restoredCluster = rds.restoreDBClusterFromSnapshot(auroraRequest);

        DBInstance restoredInstance = rds.createDBInstance(createDBInstanceRequest);
        LOG.info("Created instance {} in cluster {}", restoredInstance.getDBInstanceIdentifier(), restoredInstance.getDBClusterIdentifier());



        LOG.info("[{}] Kicked off new DB instance creation. All done here. Check on your instance progress at\n" +
                        "  https://console.aws.amazon.com/rds/home?region={}#dbinstance:id={}",
                COMMAND_NEW, cfg.region(), newInstanceIdentifier);

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoNew().call();
    }
}
