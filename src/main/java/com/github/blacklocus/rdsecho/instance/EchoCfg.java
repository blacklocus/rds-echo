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

import com.github.blacklocus.rdsecho.EchoConst;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoCfg {

    private static final Logger LOG = LoggerFactory.getLogger(EchoCfg.class);

    public static final String PREFIX = "rdsecho.";

    // Required, must be defined
    public static final String PROP_INTERACTIVE = PREFIX + "interactive";
    public static final String PROP_NAME = PREFIX + "name";
    public static final String PROP_REGION = PREFIX + "region";
    public static final String PROP_ACCOUNT_NUMBER = PREFIX + "accountNumber";
    public static final String PROP_SNAPSHOT_DB_INSTANCE_IDENTIFIER = PREFIX + "snapshot.dbInstanceIdentifier";

    // All new instance parameters are required
    public static final String PROP_NEW_ENGINE = PREFIX + "new.engine";
    public static final String PROP_NEW_LICENSE_MODEL = PREFIX + "new.licenseModel";
    public static final String PROP_NEW_DB_INSTANCE_CLASS = PREFIX + "new.dbInstanceClass";
    public static final String PROP_NEW_MULTI_AZ = PREFIX + "new.multiAz";
    public static final String PROP_NEW_STORAGE_TYPE = PREFIX + "new.storageType";
    public static final String PROP_NEW_IOPS = PREFIX + "new.iops";
    public static final String PROP_NEW_PORT = PREFIX + "new.port";
    public static final String PROP_NEW_OPTION_GROUP_NAME = PREFIX + "new.optionGroupName";
    public static final String PROP_NEW_AUTO_MINOR_VERSION_UPGRADE = PREFIX + "new.autoMinorVersionUpgrade";
    public static final String PROP_NEW_TAGS = PREFIX + "new.tags";

    // Modify parameters are mostly optional
    public static final String PROP_MOD_DB_PARAMETER_GROUP_NAME = PREFIX + "mod.dbParameterGroupName";
    public static final String PROP_MOD_DB_SECURITY_GROUPS = PREFIX + "mod.dbSecurityGroups";
    public static final String PROP_MOD_BACKUP_RETENTION_PERIOD = PREFIX + "mod.backupRetentionPeriod";
    public static final String PROP_MOD_APPLY_IMMEDIATELY = PREFIX + "mod.applyImmediately";

    // Promote parameters are required
    public static final String PROP_PROMOTE_CNAME = PREFIX + "promote.cname";
    public static final String PROP_PROMOTE_TTL = PREFIX + "promote.ttl";
    public static final String PROP_PROMOTE_TAGS = PREFIX + "promote.tags";

    // Retire parameters are optional and unspecified take on AWS defaults
    public static final String PROP_RETIRE_SKIP_FINAL_SNAPSHOT = PREFIX + "retire.skipFinalSnapshot";
    public static final String PROP_RETIRE_FINAL_DB_SNAPSHOT_IDENTIFIER = PREFIX + "retire.finalDbSnapshotIdentifier";

    final String[] required = new String[]{
            PROP_INTERACTIVE,
            PROP_NAME,
            PROP_REGION,
            PROP_ACCOUNT_NUMBER,
            PROP_SNAPSHOT_DB_INSTANCE_IDENTIFIER,
            PROP_MOD_APPLY_IMMEDIATELY,
            PROP_PROMOTE_CNAME,
            PROP_PROMOTE_TTL,
    };
    final CompositeConfiguration cfg;

    // package scoped for testing
    EchoCfg(String propertiesFilename) {
        this.cfg = new CompositeConfiguration();
        this.cfg.addConfiguration(new SystemConfiguration());
        try {
            this.cfg.addConfiguration(new PropertiesConfiguration(propertiesFilename));
            LOG.info("Reading configuration from {}", propertiesFilename);

        } catch (ConfigurationException e) {
            LOG.info("{} file not found; reading configuration from VM properties directly", propertiesFilename);
        }
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

    public String region() {
        return cfg.getString(PROP_REGION);
    }

    public String accountNumber() {
        return cfg.getString(PROP_ACCOUNT_NUMBER);
    }

    public String snapshotDbInstanceIdentifier() {
        return cfg.getString(PROP_SNAPSHOT_DB_INSTANCE_IDENTIFIER);
    }

    public Optional<String> newEngine() {
        return Optional.fromNullable(cfg.getString(PROP_NEW_ENGINE));
    }

    public Optional<String> newLicenseModel() {
        return Optional.fromNullable(cfg.getString(PROP_NEW_LICENSE_MODEL));
    }

    public Optional<String> newDbInstanceClass() {
        return Optional.fromNullable(cfg.getString(PROP_NEW_DB_INSTANCE_CLASS));
    }

    public Optional<Boolean> newMultiAz() {
        return Optional.fromNullable(cfg.getBoolean(PROP_NEW_MULTI_AZ, null));
    }

    public Optional<String> newStorageType() {
        return Optional.fromNullable(cfg.getString(PROP_NEW_STORAGE_TYPE));
    }

    public Optional<Integer> newIops() {
        return Optional.fromNullable(cfg.getInteger(PROP_NEW_IOPS, null));
    }

    public Optional<Integer> newPort() {
        return Optional.fromNullable(cfg.getInteger(PROP_NEW_PORT, null));
    }

    public Optional<String> newOptionGroupName() {
        return Optional.fromNullable(cfg.getString(PROP_NEW_OPTION_GROUP_NAME));
    }

    public Optional<Boolean> newAutoMinorVersionUpgrade() {
        return Optional.fromNullable(cfg.getBoolean(PROP_NEW_AUTO_MINOR_VERSION_UPGRADE, null));
    }

    public Optional<String[]> newTags() {
        String[] values = cfg.getStringArray(PROP_NEW_TAGS);
        if (values == null || values.length == 0) {
            return Optional.absent();
        } else {
            return Optional.of(values);
        }
    }

    public Optional<String> modDbParameterGroupName() {
        return Optional.fromNullable(cfg.getString(PROP_MOD_DB_PARAMETER_GROUP_NAME));
    }

    public Optional<String[]> modDbSecurityGroups() {
        String[] values = cfg.getStringArray(PROP_MOD_DB_SECURITY_GROUPS);
        if (values == null || values.length == 0) {
            return Optional.absent();
        } else {
            return Optional.of(values);
        }
    }

    public Optional<Integer> modBackupRetentionPeriod() {
        return Optional.fromNullable(cfg.getInteger(PROP_MOD_BACKUP_RETENTION_PERIOD, null));
    }

    public boolean modApplyImmediately() {
        return cfg.getBoolean(PROP_MOD_APPLY_IMMEDIATELY);
    }

    public String promoteCname() {
        return cfg.getString(PROP_PROMOTE_CNAME);
    }

    public long promoteTtl() {
        return cfg.getLong(PROP_PROMOTE_TTL);
    }

    public Optional<String[]> promoteTags() {
        String[] values = cfg.getStringArray(PROP_PROMOTE_TAGS);
        if (values == null || values.length == 0) {
            return Optional.absent();
        } else {
            return Optional.of(values);
        }
    }

    public Optional<Boolean> retireSkipFinalSnapshot() {
        return Optional.fromNullable(cfg.getBoolean(PROP_RETIRE_SKIP_FINAL_SNAPSHOT, null));
    }

    public Optional<String> retireFinalDbSnapshotIdentifier() {
        return Optional.fromNullable(cfg.getString(PROP_RETIRE_FINAL_DB_SNAPSHOT_IDENTIFIER));
    }

    public static final class Lazy {
        static final EchoCfg INSTANCE = new EchoCfg(EchoConst.CONFIGURATION_PROPERTIES);
    }

    public static EchoCfg getInstance() {
        return Lazy.INSTANCE;
    }
}
