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
