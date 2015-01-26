package com.blacklocus.rds.utl;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.Tag;
import com.blacklocus.rds.EchoCfg;
import com.blacklocus.rds.EchoConst;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RdsEchoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RdsEchoUtil.class);

    final RdsFind rdsFind = new RdsFind();

    final EchoCfg cfg = new EchoCfg();

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
