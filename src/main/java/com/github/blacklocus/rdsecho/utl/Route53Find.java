package com.github.blacklocus.rdsecho.utl;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesRequest;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import java.util.Collections;


public class Route53Find {
    final AmazonRoute53 route53 = new AmazonRoute53Client();

    public Optional<HostedZone> hostedZone() {
        return Optional.fromNullable(Iterables.getFirst(hostedZones(), null));
    }

    public Iterable<HostedZone> hostedZones() {
        return hostedZones(Predicates.<HostedZone>alwaysTrue());
    }

    public Optional<HostedZone> hostedZone(final Predicate<HostedZone> predicate) {
        return Optional.fromNullable(Iterables.getFirst(hostedZones(predicate), null));
    }

    public Iterable<HostedZone> hostedZones(final Predicate<HostedZone> predicate) {
        return new PagingIterable<HostedZone>(new Supplier<Iterable<HostedZone>>() {

            String nextMarker = null;
            boolean isTruncated = true;

            @Override
            public Iterable<HostedZone> get() {
                if (isTruncated) {
                    ListHostedZonesRequest request = new ListHostedZonesRequest()
                            .withMarker(nextMarker);
                    ListHostedZonesResult result = route53.listHostedZones();
                    nextMarker = result.getNextMarker();
                    isTruncated = result.isTruncated();
                    return Iterables.filter(result.getHostedZones(), predicate);

                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    public Optional<ResourceRecordSet> resourceRecordSet(String hostedZoneId) {
        return Optional.fromNullable(Iterables.getFirst(resourceRecordSets(hostedZoneId), null));
    }

    public Iterable<ResourceRecordSet> resourceRecordSets(String hostedZoneId) {
        return resourceRecordSets(hostedZoneId, Predicates.<ResourceRecordSet>alwaysTrue());
    }

    public Optional<ResourceRecordSet> resourceRecordSet(final String hostedZoneId, final Predicate<ResourceRecordSet> predicate) {
        return Optional.fromNullable(Iterables.getFirst(resourceRecordSets(hostedZoneId, predicate), null));
    }

    public Iterable<ResourceRecordSet> resourceRecordSets(final String hostedZoneId, final Predicate<ResourceRecordSet> predicate) {
        return new PagingIterable<ResourceRecordSet>(new Supplier<Iterable<ResourceRecordSet>>() {

            String nextRecordName = null;
            boolean isTruncated = true;

            @Override
            public Iterable<ResourceRecordSet> get() {
                if (isTruncated) {
                    ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest()
                            .withHostedZoneId(hostedZoneId)
                            .withStartRecordName(nextRecordName);
                    ListResourceRecordSetsResult result = route53.listResourceRecordSets(request);
                    nextRecordName = result.getNextRecordName();
                    isTruncated = result.isTruncated();
                    return Iterables.filter(result.getResourceRecordSets(), predicate);

                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    public static Predicate<HostedZone> nameEquals(final String name) {
        return new Predicate<HostedZone>() {
            @Override
            public boolean apply(HostedZone hostedZone) {
                return hostedZone.getName().equals(name);
            }
        };
    }

    public static Predicate<ResourceRecordSet> cnameEquals(final String cname) {
        return new Predicate<ResourceRecordSet>() {
            @Override
            public boolean apply(ResourceRecordSet resourceRecordSet) {
                return resourceRecordSet.getName().equals(cname) &&
                        resourceRecordSet.getType().equals("CNAME");
            }
        };
    }

}
