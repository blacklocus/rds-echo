package com.github.blacklocus.rdsecho.utl;

import com.amazonaws.services.rds.model.Tag;
import com.github.blacklocus.rdsecho.instance.utl.EchoUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class EchoUtilTest {

    @Test
    public void parseTags() {
        String[] rawTags = {"development=yes", "potato=no", "tomato", "pterodactyl=well=maybe"};
        List<Tag> tags = EchoUtil.parseTags(rawTags);

        Assert.assertEquals(3, tags.size());

        Tag one = tags.get(0);
        Assert.assertEquals("development", one.getKey());
        Assert.assertEquals("yes", one.getValue());

        Tag two = tags.get(1);
        Assert.assertEquals("potato", two.getKey());
        Assert.assertEquals("no", two.getValue());

        Tag three = tags.get(2);
        Assert.assertEquals("pterodactyl", three.getKey());
        Assert.assertEquals("well=maybe", three.getValue());
    }
}
