package com.blacklocus.rds;

import com.google.common.base.Preconditions;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;

public class DbEchoCfg {

    public static final String PROP_PFX = "bl.dbecho.";
    public static final String PROP_INTERACTIVE = PROP_PFX + "interactive";
    public static final String PROP_NAME = PROP_PFX + "name";
    public static final String PROP_CNAME = PROP_PFX + "cname";
    public static final String PROP_REGION = PROP_PFX + "region";
    public static final String PROP_ACCOUNT_NUMBER = PROP_PFX + "accountNumber";
    public static final String PROP_SNAPSHOT_DB_INSTANCE_IDENTIFIER = PROP_PFX + "snapshot.dbInstanceIdentifier";
    public static final String PROP_NEW_ENGINE = PROP_PFX + "new.engine";
    public static final String PROP_NEW_LICENSE_MODEL = PROP_PFX + "new.licenseModel";
    public static final String PROP_NEW_DB_INSTANCE_CLASS = PROP_PFX + "new.dbInstanceClass";
    public static final String PROP_NEW_MULTI_AZ = PROP_PFX + "new.multiAz";
    public static final String PROP_NEW_STORAGE_TYPE = PROP_PFX + "new.storageType";
    public static final String PROP_NEW_IOPS = PROP_PFX + "new.iops";
    public static final String PROP_NEW_PORT = PROP_PFX + "new.port";
    public static final String PROP_NEW_OPTION_GROUP_NAME = PROP_PFX + "new.optionGroupName";
    public static final String PROP_NEW_AUTO_MINOR_VERSION_UPGRADE = PROP_PFX + "new.autoMinorVersionUpgrade";

    final String[] required = new String[]{PROP_CNAME};
    final Configuration cfg = new SystemConfiguration();

    public DbEchoCfg() {
        validate();
    }

    void validate() {
        for (String prop : required) {
            Preconditions.checkState(cfg.containsKey(prop), prop + " must be defined");
        }
    }

    public boolean interactive() {
        return cfg.getBoolean(PROP_INTERACTIVE);
    }

    public String name() {
        return cfg.getString(PROP_NAME);
    }

    public String cname() {
        return cfg.getString(PROP_CNAME);
    }

    public String region() {
        return cfg.getString(PROP_REGION);
    }

    public String accountNumber() {
        return cfg.getString(PROP_ACCOUNT_NUMBER);
    }

    public String snapshotDbInstanceIdentifier() {
        return cfg.getString(PROP_SNAPSHOT_DB_INSTANCE_IDENTIFIER);
    }

    public String newEngine() {
        return cfg.getString(PROP_NEW_ENGINE);
    }

    public String newLicenseModel() {
        return cfg.getString(PROP_NEW_LICENSE_MODEL);
    }

    public String newDbInstanceClass() {
        return cfg.getString(PROP_NEW_DB_INSTANCE_CLASS);
    }

    public boolean newMultiAz() {
        return cfg.getBoolean(PROP_NEW_MULTI_AZ);
    }

    public String newStorageType() {
        return cfg.getString(PROP_NEW_STORAGE_TYPE);
    }

    public int newIops() {
        return cfg.getInt(PROP_NEW_IOPS);
    }

    public int newPort() {
        return cfg.getInt(PROP_NEW_PORT);
    }

    public String newOptionGroupName() {
        return cfg.getString(PROP_NEW_OPTION_GROUP_NAME);
    }

    public boolean newAutoMinorVersionUpgrade() {
        return cfg.getBoolean(PROP_NEW_AUTO_MINOR_VERSION_UPGRADE);
    }
}
