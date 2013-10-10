/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;

import com.sun.syndication.feed.synd.SyndFeed;

import java.io.DataInputStream;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TransformerDiscoveryTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testLookup() throws Exception
    {
        Transformer t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.create(DataInputStream.class), DataTypeFactory.create(SyndFeed.class));
        assertNotNull(t);
        t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.create(DataInputStream.class), DataTypeFactory.create(SyndFeed.class));
        assertNotNull(t);
    }
}
