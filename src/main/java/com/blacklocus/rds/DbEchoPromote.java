package com.blacklocus.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.blacklocus.rds.utl.DbEchoUtil;
import com.blacklocus.rds.utl.Route53Find;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static com.blacklocus.rds.utl.Route53Find.cnameEquals;
import static com.blacklocus.rds.utl.Route53Find.nameEquals;
import static com.google.common.collect.Iterables.getOnlyElement;

/**
 *
 */
public class DbEchoPromote implements Callable<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(DbEchoPromote.class);

    final AmazonRDS rds = new AmazonRDSClient();
    final Route53Find route53Find = new Route53Find();

    final DbEchoCfg cfg = new DbEchoCfg();
    final DbEchoUtil echo = new DbEchoUtil();

    @Override
    public Void call() throws Exception {

        String tagEchoManaged = echo.getTagEchoManaged();

        LOG.info("Locating last echo instance with tag {}", tagEchoManaged);
        Optional<DBInstance> newestInstanceOpt = echo.lastEchoInstance();
        if (!newestInstanceOpt.isPresent()) {
            LOG.error("  There is no echo instance tagged with {}. Cannot promote nothing to CNAME {}. Exiting.",
                    tagEchoManaged, cfg.cname());
            return null;
        }

        LOG.info("Reading current DNS records");
        String tld = DbEchoUtil.getTLD(cfg.cname()) + '.';
        HostedZone hostedZone = route53Find.hostedZone(nameEquals(tld)).get();
        LOG.info("  Found corresponding HostedZone. name: {} id: {}", hostedZone.getName(), hostedZone.getId());

        ResourceRecordSet resourceRecordSet = route53Find.resourceRecordSet(
                hostedZone.getId(), cnameEquals(cfg.cname())).get();
        ResourceRecord resourceRecord = getOnlyElement(resourceRecordSet.getResourceRecords());
        LOG.info("  Found CNAME {} with current value {}", resourceRecordSet.getName(), resourceRecord.getValue());

        if (newestInstanceOpt.isPresent()) {
            DBInstance instance = newestInstanceOpt.get();
            Endpoint endpoint = instance.getEndpoint();
            if (null == endpoint) {
                LOG.info("Echo DB instance {} (id: {}) has no address. Is it still initializing?",
                        tagEchoManaged, instance.getDBInstanceIdentifier());
                return null;
            }
            String echoInstanceAddr = endpoint.getAddress();
            if (resourceRecord.getValue().equals(echoInstanceAddr)) {
                LOG.info("  Echo DB instance {} ({}) lines up with CNAME {}.",
                        tagEchoManaged, echoInstanceAddr, resourceRecordSet.getName());
            } else {
                LOG.info("  Echo DB instance {} ({}) differs from CNAME {}.",
                        tagEchoManaged, echoInstanceAddr, resourceRecordSet.getName());
            }
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        new DbEchoPromote().call();
    }
}
