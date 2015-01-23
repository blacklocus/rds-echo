package com.blacklocus.rds;

import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.blacklocus.rds.utl.Route53Find;
import org.testng.annotations.Test;

public class AWSFindTest {

    Route53Find find = new Route53Find();

    @Test
    public void testHostedZonesAndResourceRecordSets() throws Exception {
        for (HostedZone hostedZone : find.hostedZones()) {
            System.out.println(hostedZone);
            for (ResourceRecordSet rrs : find.resourceRecordSets(hostedZone.getId())) {
                System.out.println(rrs);
            }

        }
    }
}