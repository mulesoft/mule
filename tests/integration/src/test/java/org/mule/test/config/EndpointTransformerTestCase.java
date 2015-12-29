/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.api.NameableObject;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.functional.transformer.NoActionTransformer;

import org.junit.Test;

public class EndpointTransformerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            "test:///tmp?transformers=NoActionTransformer");
        assertEquals("NoActionTransformer", ((NameableObject) endpoint.getMessageProcessors().get(0)).getName());
    }

    @Test
    public void testResponseTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            "test:///tmp?responseTransformers=NoActionTransformer");
        assertEquals("NoActionTransformer", ((NameableObject) endpoint.getResponseMessageProcessors().get(0)).getName());
    }
}
