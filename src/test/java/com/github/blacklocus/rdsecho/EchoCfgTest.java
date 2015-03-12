package com.github.blacklocus.rdsecho;

import com.google.common.base.Optional;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EchoCfgTest {

    @Test
    public void checkSample() {
        EchoCfg cfg = new EchoCfg("rdsecho.properties.sample");

        Optional<String[]> tags = cfg.promoteTags();
        Assert.assertTrue(tags.isPresent());

        String[] allTags = tags.get();
        Assert.assertEquals(2, allTags.length);

        Assert.assertEquals("development=yes", allTags[0]);
        Assert.assertEquals("banana=no", allTags[1]);
    }
}
