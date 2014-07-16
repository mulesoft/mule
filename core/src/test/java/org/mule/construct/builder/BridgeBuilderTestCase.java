/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.builder;

import static org.junit.Assert.assertEquals;

import org.mule.MessageExchangePattern;
import org.mule.construct.Bridge;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.compression.GZipCompressTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;

import org.junit.Test;

public class BridgeBuilderTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testFullConfiguration() throws Exception
    {
        Bridge bridge = new BridgeBuilder().name("test-bridge-full")
            .inboundAddress("test://foo.in")
            .transformers(new StringAppendTransformer("bar"))
            .responseTransformers(new ObjectToByteArray(), new GZipCompressTransformer())
            .outboundAddress("test://foo.out")
            .exchangePattern(MessageExchangePattern.REQUEST_RESPONSE)
            .transacted(false)
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext))
            .build(muleContext);

        assertEquals("test-bridge-full", bridge.getName());
    }

    @Test
    public void testTransacted() throws Exception
    {
        Bridge bridge = new BridgeBuilder().name("test-bridge-transacted")
            .inboundAddress("test://foo.in")
            .outboundAddress("test2://foo.out")
            .transacted(true)
            .build(muleContext);

        assertEquals("test-bridge-transacted", bridge.getName());
    }
}
