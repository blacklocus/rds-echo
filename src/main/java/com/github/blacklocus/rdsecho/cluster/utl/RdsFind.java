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
package com.github.blacklocus.rdsecho.cluster.utl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.amazonaws.services.rds.model.Tag;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class RdsFind {

    final AmazonRDS rds = new AmazonRDSClient();

    // Retry 10 times with exponential backoff, starting with 1 second bounded to 60 seconds
    final Retryer<ListTagsForResourceResult> tagRetryer = RetryerBuilder.<ListTagsForResourceResult>newBuilder()
            .retryIfExceptionOfType(AmazonServiceException.class)
            .retryIfRuntimeException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(10))
            .withWaitStrategy(WaitStrategies.exponentialWait(1, 60, TimeUnit.SECONDS))
            .build();

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

    public Optional<Tag> instanceTag(String instanceArn, Predicate<Tag> predicate) {
        return Optional.fromNullable(Iterables.getFirst(instanceTags(instanceArn, predicate), null));
    }

    public Iterable<Tag> instanceTags(String instanceArn, Predicate<Tag> predicate) {
        ListTagsForResourceResult result = rds.listTagsForResource(new ListTagsForResourceRequest()
                .withResourceName(instanceArn));
        return Iterables.filter(result.getTagList(), predicate);
    }

    public Iterable<DBClusterSnapshot> snapshots(final String dbInstanceIdentifier, final Predicate<DBClusterSnapshot> predicate) {
        return new PagingIterable<>(new Supplier<Iterable<DBClusterSnapshot>>() {

            String marker = null;
            boolean isTruncated = true;

            @Override
            public Iterable<DBClusterSnapshot> get() {
                if (isTruncated) {
                    DescribeDBClusterSnapshotsRequest request = new DescribeDBClusterSnapshotsRequest()
                            .withDBClusterIdentifier(dbInstanceIdentifier)
                            .withMarker(marker);
                    DescribeDBClusterSnapshotsResult result = rds.describeDBClusterSnapshots(request);
                    marker = result.getMarker();
                    isTruncated = result.getMarker() != null;
                    return Iterables.filter(result.getDBClusterSnapshots(), predicate);

                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    public Predicate<DBInstance> instanceHasTag(final String region, final String accountNumber,
                                                final String tagKey, final String tagValue) {
        return new Predicate<DBInstance>() {
            @Override
            public boolean apply(DBInstance instance) {
                try {
                    final String rdsInstanceArn = instanceArn(region, accountNumber, instance.getDBInstanceIdentifier());
                    ListTagsForResourceResult result = tagRetryer.call(new Callable<ListTagsForResourceResult>() {
                        @Override
                        public ListTagsForResourceResult call() throws Exception {
                            return rds.listTagsForResource(new ListTagsForResourceRequest()
                                    .withResourceName(rdsInstanceArn));
                        }
                    });

                    return Iterables.any(result.getTagList(), new Predicate<Tag>() {
                        @Override
                        public boolean apply(Tag tag) {
                            return tagKey.equals(tag.getKey()) && tagValue.equals(tag.getValue());
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static String instanceArn(String region, String accountNumber, String dbInstanceIdentifier) {
        return String.format("arn:aws:rds:%s:%s:db:%s", region, accountNumber, dbInstanceIdentifier);
    }

    public static Predicate<DBClusterSnapshot> snapshotIsAvailable() {
        return new Predicate<DBClusterSnapshot>() {
            @Override
            public boolean apply(DBClusterSnapshot snapshot) {
                return "available".equals(snapshot.getStatus());
            }
        };
    }

    public static Predicate<Tag> tagName(final String name) {
        return new Predicate<Tag>() {
            @Override
            public boolean apply(Tag tag) {
                return tag.getKey().equals(name);
            }
        };
    }

    public static Optional<DBInstance> newestInstance(Iterable<DBInstance> instances) {
        DBInstance newest = null;

        // filter out instances without a create time
        Iterable<DBInstance> validInstances = Iterables.filter(instances, new Predicate<DBInstance>() {
            @Override
            public boolean apply(@Nullable DBInstance input) {
                return input != null && input.getInstanceCreateTime() != null;
            }
        });

        for (DBInstance instance : validInstances) {
            if (newest == null || instance.getInstanceCreateTime().after(newest.getInstanceCreateTime())) {
                newest = instance;
            }
        }
        return Optional.fromNullable(newest);
    }

    public static Optional<DBClusterSnapshot> newestSnapshot(Iterable<DBClusterSnapshot> snapshots) {
        DBClusterSnapshot newest = null;

        // filter out instances without a create time
        Iterable<DBClusterSnapshot> validSnapshots = Iterables.filter(snapshots, new Predicate<DBClusterSnapshot>() {
            @Override
            public boolean apply(@Nullable DBClusterSnapshot input) {
                return input != null && input.getSnapshotCreateTime() != null;
            }
        });

        for (DBClusterSnapshot snapshot : validSnapshots) {
            if (newest == null || snapshot.getSnapshotCreateTime().after(newest.getSnapshotCreateTime())) {
                newest = snapshot;
            }
        }
        return Optional.fromNullable(newest);
    }

}
