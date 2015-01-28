package com.github.blacklocus.rdsecho.utl;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.EchoCfg;
import com.github.blacklocus.rdsecho.EchoConst;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EchoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(EchoUtil.class);

    final RdsFind rdsFind = new RdsFind();

    final EchoCfg cfg = EchoCfg.getInstance();

    public String getTagEchoManaged() {
        return String.format(EchoConst.TAG_ECHO_MANAGED_FMT, cfg.name());
    }

    public String getTagEchoStage() {
        return String.format(EchoConst.TAG_ECHO_STAGE_FMT, cfg.name());
    }

    public Optional<DBInstance> lastEchoInstance() {
        return RdsFind.newestInstance(rdsFind.instances(rdsFind.instanceHasTag(
                cfg.region(), cfg.accountNumber(), getTagEchoManaged(), "true")));
    }

    public Optional<DBInstance> promotedInstance() {
        return Optional.fromNullable(Iterables.getOnlyElement(rdsFind.instances(rdsFind.instanceHasTag(
                cfg.region(), cfg.accountNumber(), getTagEchoStage(), EchoConst.STAGE_PROMOTED
        )), null));
    }

    public Optional<Tag> instanceStage(String dbInstanceIdentifier) {
        return rdsFind.instanceTag(
                RdsFind.instanceArn(cfg.region(), cfg.accountNumber(), dbInstanceIdentifier),
                RdsFind.tagName(getTagEchoStage()));
    }

    public Optional<DBSnapshot> latestSnapshot() {
        return RdsFind.newestSnapshot(rdsFind.snapshots(cfg.snapshotDbInstanceIdentifier(), RdsFind.snapshotIsAvailable()));
    }

    public static String getTLD(String domain) {
        String[] split = domain.split("\\.");
        return split[split.length - 2] + '.' + split[split.length - 1];
    }

    public static boolean prompt(String confirm, String format, Object... args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        LOG.info(String.format(format, args));
        try {
            String userConfirm = reader.readLine();
            return confirm.equals(userConfirm);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
