package com.github.blacklocus.rdsecho.instance;

import com.google.common.base.Optional;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EchoCfgTest {

    @Test
    public void checkSample() {
        EchoCfg cfg = new EchoCfg("rdsecho.properties.sample");

        Optional<String[]> newTags = cfg.newTags();
        Assert.assertTrue(newTags.isPresent());

        String[] allNewTags = newTags.get();
        Assert.assertEquals(2, allNewTags.length);

        Assert.assertEquals("orange=false", allNewTags[0]);
        Assert.assertEquals("pear=maybe", allNewTags[1]);

        Optional<String[]> promoteTags = cfg.promoteTags();
        Assert.assertTrue(promoteTags.isPresent());

        String[] allPromoteTags = promoteTags.get();
        Assert.assertEquals(2, allPromoteTags.length);

        Assert.assertEquals("development=yes", allPromoteTags[0]);
        Assert.assertEquals("banana=no", allPromoteTags[1]);
    }
}
