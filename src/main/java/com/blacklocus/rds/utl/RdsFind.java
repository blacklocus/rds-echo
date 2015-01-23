package com.blacklocus.rds.utl;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.amazonaws.services.rds.model.Tag;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import java.util.Collections;


public class RdsFind {

    final AmazonRDS rds = new AmazonRDSClient();

    public Optional<DBInstance> instance(Predicate<DBInstance> predicate) {
        return Optional.fromNullable(Iterables.getFirst(instances(predicate), null));
    }

    public Iterable<DBInstance> instances(final Predicate<DBInstance> predicate) {
        return new PagingIterable<DBInstance>(new Supplier<Iterable<DBInstance>>() {

            String marker = null;
            boolean isTruncated = true;

            @Override
            public Iterable<DBInstance> get() {
                if (isTruncated) {
                    DescribeDBInstancesRequest request = new DescribeDBInstancesRequest()
                            .withMarker(marker);
                    DescribeDBInstancesResult result = rds.describeDBInstances(request);
                    marker = result.getMarker();
                    isTruncated = result.getMarker() != null;
                    return Iterables.filter(result.getDBInstances(), predicate);

                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    public Iterable<DBSnapshot> snapshots(final String dbInstanceIdentifier, final Predicate<DBSnapshot> predicate) {
        return new PagingIterable<DBSnapshot>(new Supplier<Iterable<DBSnapshot>>() {

            String marker = null;
            boolean isTruncated = true;

            @Override
            public Iterable<DBSnapshot> get() {
                if (isTruncated) {
                    DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest()
                            .withDBInstanceIdentifier(dbInstanceIdentifier)
                            .withMarker(marker);
                    DescribeDBSnapshotsResult result = rds.describeDBSnapshots(request);
                    marker = result.getMarker();
                    isTruncated = result.getMarker() != null;
                    return Iterables.filter(result.getDBSnapshots(), predicate);

                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    public Predicate<DBInstance> instanceHasTagKey(final String region, final String accountNumber, final String tagKey) {
        return new Predicate<DBInstance>() {
            @Override
            public boolean apply(DBInstance instance) {
                String rdsInstanceArn = String.format("arn:aws:rds:%s:%s:db:%s", region, accountNumber, instance.getDBInstanceIdentifier());
                ListTagsForResourceResult result = rds.listTagsForResource(new ListTagsForResourceRequest()
                        .withResourceName(rdsInstanceArn));

                return Iterables.any(result.getTagList(), new Predicate<Tag>() {
                    @Override
                    public boolean apply(Tag tag) {
                        return tagKey.equals(tag.getKey());
                    }
                });
            }
        };
    }

    public static Predicate<DBSnapshot> snapshotIsAvailable() {
        return new Predicate<DBSnapshot>() {
            @Override
            public boolean apply(DBSnapshot snapshot) {
                return "available".equals(snapshot.getStatus());
            }
        };
    }

    public static Optional<DBInstance> newestInstance(Iterable<DBInstance> instances) {
        DBInstance newest = null;
        for (DBInstance instance : instances) {
            if (newest == null || instance.getInstanceCreateTime().after(newest.getInstanceCreateTime())) {
                newest = instance;
            }
        }
        return Optional.fromNullable(newest);
    }

    public static Optional<DBSnapshot> newestSnapshot(Iterable<DBSnapshot> snapshots) {
        DBSnapshot newest = null;
        for (DBSnapshot snapshot : snapshots) {
            if (newest == null || snapshot.getInstanceCreateTime().after(newest.getInstanceCreateTime())) {
                newest = snapshot;
            }
        }
        return Optional.fromNullable(newest);

    }
}
