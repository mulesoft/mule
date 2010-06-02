/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.api.transformer.Transformer;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.types.DataTypeFactory;

import com.sun.syndication.feed.synd.SyndFeed;

import java.io.DataInputStream;

public class TransformerDiscoveryTestCase extends AbstractMuleTestCase
{
    public void testLookup() throws Exception
    {
        Transformer t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.create(DataInputStream.class), DataTypeFactory.create(SyndFeed.class));
        assertNotNull(t);
        t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.create(DataInputStream.class), DataTypeFactory.create(SyndFeed.class));
        assertNotNull(t);
    }
}
