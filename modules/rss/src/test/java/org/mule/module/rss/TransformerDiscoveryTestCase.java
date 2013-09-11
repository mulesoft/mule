/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
