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

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.ModifyDBClusterRequest;
import com.github.blacklocus.rdsecho.cluster.utl.EchoUtil;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class EchoModify extends AbstractEchoIntermediateStage {

    private static final Logger LOG = LoggerFactory.getLogger(EchoModify.class);

    public EchoModify() {
        super(EchoConst.STAGE_NEW, EchoConst.STAGE_MODIFIED);
    }

    @Override
    boolean traverseStage(DBInstance instance) {

        // Prepare request and build up informational message with conditional parts.
        String dbInstanceId = instance.getDBInstanceIdentifier();

        StringWriter proposed = new StringWriter();
        PrintWriter printer = new PrintWriter(proposed);
        printer.format("[%s] Proposed db modifications on instance %s...%n", getCommand(), dbInstanceId);

        ModifyDBClusterRequest auroraRequest = new ModifyDBClusterRequest();

        auroraRequest.withDBClusterIdentifier(instance.getDBClusterIdentifier());

        Optional<String> dbParameterGroupNameOpt = cfg.modDbParameterGroupName();
        if (dbParameterGroupNameOpt.isPresent()) {
            auroraRequest.withDBClusterParameterGroupName(dbParameterGroupNameOpt.get());
            printer.format("  db param group name    : %s%n", dbParameterGroupNameOpt.get());
        }
        Optional<String[]> dbSecurityGroupsOpt = cfg.modDbSecurityGroups();
        if (dbSecurityGroupsOpt.isPresent()) {
            auroraRequest.withVpcSecurityGroupIds(dbSecurityGroupsOpt.get());
            printer.format("  vpc security groups    : %s%n", Arrays.asList(dbSecurityGroupsOpt.get()));
        }
        Optional<Integer> backupRetentionPeriodOpt = cfg.modBackupRetentionPeriod();
        if (backupRetentionPeriodOpt.isPresent()) {
            auroraRequest.withBackupRetentionPeriod(backupRetentionPeriodOpt.get());
            printer.format("  backup retention period: %d%n", backupRetentionPeriodOpt.get());
        }
        boolean applyImmediately = cfg.modApplyImmediately();
        printer.format("  apply immediately      : %b%n", applyImmediately);
        auroraRequest.withApplyImmediately(applyImmediately);

//        auroraRequest.withOptionGroupName() // TODO
        LOG.info(proposed.toString());

        // Interactive user confirm

        if (cfg.interactive()) {
            String format = "Proceed to modify DB instance with these settings? Input %s to confirm.";
            if (!EchoUtil.prompt(dbInstanceId, format, dbInstanceId)) {
                LOG.info("User declined to proceed. Exiting.");
                return false;
            }
        }

        // Do the deed

        LOG.info("[{}] Modifying existing DB instance {}", getCommand(), dbInstanceId);
        rds.modifyDBCluster(auroraRequest);
        LOG.info("[{}] Submitted modify request on instance {}. Finished.", getCommand(), dbInstanceId);

        return true;
    }

    @Override
    String getCommand() {
        return EchoConst.COMMAND_MODIFY;
    }

    public static void main(String[] args) throws Exception {
        new EchoModify().call();
    }
}