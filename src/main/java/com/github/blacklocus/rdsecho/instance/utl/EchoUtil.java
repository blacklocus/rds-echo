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
package com.github.blacklocus.rdsecho.instance.utl;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.instance.EchoCfg;
import com.github.blacklocus.rdsecho.EchoConst;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

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

    public static List<Tag> parseTags(String[] rawTags) {
        List<Tag> tags = Lists.newArrayList();
        for(String rawTag : rawTags) {
            String[] splitTag = rawTag.split("=", 2);
            if(splitTag.length == 2) {
                tags.add(new Tag().withKey(splitTag[0]).withValue(splitTag[1]));
            }
        }
        return tags;
    }
}
