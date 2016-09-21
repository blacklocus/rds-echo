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
package com.github.blacklocus.rdsecho.instance;


import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.EchoConst;
import com.github.blacklocus.rdsecho.instance.utl.EchoUtil;
import com.github.blacklocus.rdsecho.instance.utl.RdsFind;
import com.google.common.base.Optional;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        LOG.info("[{}] Locating latest snapshot from {}", COMMAND_NEW, cfg.snapshotDbInstanceIdentifier());
        Optional<DBSnapshot> dbSnapshotOpt = echo.latestSnapshot();
        if (dbSnapshotOpt.isPresent()) {
            DBSnapshot snapshot = dbSnapshotOpt.get();
            LOG.info("[{}] Located snapshot {} completed on {}", COMMAND_NEW, snapshot.getDBSnapshotIdentifier(),
                    new DateTime(snapshot.getSnapshotCreateTime()).toDateTimeISO().toString());

        } else {
            LOG.info("[{}] Could not locate a suitable snapshot. Cannot continue.", COMMAND_NEW);
            return false;
        }

        // Info summary

        String dbSnapshotIdentifier = dbSnapshotOpt.get().getDBSnapshotIdentifier();
        String newDbInstanceIdentifier = cfg.name() + '-' + DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd");

        // Prepare request and build up informational message with conditional parts.

        StringWriter proposed = new StringWriter();
        PrintWriter printer = new PrintWriter(proposed);
        printer.format("[%s] Proposed new db instance...%n", COMMAND_NEW);
        RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest();

        // Required settings

        request.withDBInstanceIdentifier(newDbInstanceIdentifier);
        request.withDBSnapshotIdentifier(dbSnapshotIdentifier);
        request.withTags(
                new Tag().withKey(echo.getTagEchoManaged()).withValue("true"),
                new Tag().withKey(echo.getTagEchoStage()).withValue(EchoConst.STAGE_NEW)
        );

        printer.format("  db snapshot id   : %s%n", dbSnapshotIdentifier);
        printer.format("  db instance id   : %s%n", newDbInstanceIdentifier);

        // Not required; these will default to snapshot settings

        Optional<String> engineOpt = cfg.newEngine();
        if (engineOpt.isPresent()) {
            request.withEngine(engineOpt.get());
            printer.format("  engine           : %s%n", engineOpt.get());
        }

        Optional<String> licenseModelOpt = cfg.newLicenseModel();
        if (licenseModelOpt.isPresent()) {
            request.withLicenseModel(licenseModelOpt.get());
            printer.format("  license model    : %s%n", licenseModelOpt.get());
        }

        Optional<String> instanceClassOpt = cfg.newDbInstanceClass();
        if (instanceClassOpt.isPresent()) {
            request.withDBInstanceClass(instanceClassOpt.get());
            printer.format("  db instance class: %s%n", instanceClassOpt.get());
        }

        Optional<Boolean> multiAzOpt = cfg.newMultiAz();
        if (multiAzOpt.isPresent()) {
            request.withMultiAZ(multiAzOpt.get());
            printer.format("  multi az         : %s%n", multiAzOpt.get());
        }

        Optional<String> storageTypeOpt = cfg.newStorageType();
        if (storageTypeOpt.isPresent()) {
            request.withStorageType(storageTypeOpt.get());
            printer.format("  storage type     : %s%n", storageTypeOpt.get());
        }

        Optional<Integer> iopsOpt = cfg.newIops();
        if (iopsOpt.isPresent()) {
            request.withIops(iopsOpt.get());
            printer.format("  iops             : %s%n", iopsOpt.get());
        }

        Optional<Integer> portOpt = cfg.newPort();
        if(portOpt.isPresent()) {
            request.withPort(portOpt.get());
            printer.format("  port             : %s%n", portOpt.get());
        }

        Optional<String> optionGroupNameOpt = cfg.newOptionGroupName();
        if (optionGroupNameOpt.isPresent()) {
            request.withOptionGroupName(optionGroupNameOpt.get());
            printer.format("  option group name: %s%n", optionGroupNameOpt.get());
        }

        Optional<Boolean> autoMinorVersionOpt = cfg.newAutoMinorVersionUpgrade();
        if (autoMinorVersionOpt.isPresent()) {
            request.withAutoMinorVersionUpgrade(autoMinorVersionOpt.get());
            printer.format("  auto minor ver up: %s%n", autoMinorVersionOpt.get());
        }

        LOG.info(proposed.toString());

        // Interactive user confirmation

        if (cfg.interactive()) {
            String format = "Proceed to create a new DB instance from this snapshot? Input %s to confirm.";
            if (!EchoUtil.prompt(newDbInstanceIdentifier, format, newDbInstanceIdentifier)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        // Create the new database

        LOG.info("[{}] Creating new DB instance. Hold on to your butts.", COMMAND_NEW);
        DBInstance restoredInstance = rds.restoreDBInstanceFromDBSnapshot(request);

        Optional<String[]> newTags = cfg.newTags();
        if (newTags.isPresent()) {
            List<Tag> tags = EchoUtil.parseTags(newTags.get());
            if (tags.size() > 0) {
                LOG.info("[{}] Applying tags on create new: {}", COMMAND_NEW, Arrays.asList(tags));
                AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                        .withResourceName(RdsFind.instanceArn(cfg.region(), cfg.accountNumber(),
                                restoredInstance.getDBInstanceIdentifier()));
                tagsRequest.setTags(tags);
                rds.addTagsToResource(tagsRequest);
            }
        }

        LOG.info("[{}] Kicked off new DB instance creation. All done here. Check on your instance progress at\n" +
                        "  https://console.aws.amazon.com/rds/home?region={}#dbinstance:id={}",
                COMMAND_NEW, cfg.region(), newDbInstanceIdentifier);

        return true;
    }

    public static void main(String[] args) throws Exception {
        new EchoNew().call();
    }
}
