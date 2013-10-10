/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.transformer.NoActionTransformer;
import org.mule.transformer.TransformerUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndpointTransformerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            "test:///tmp?transformers=NoActionTransformer");
        assertEquals("NoActionTransformer", TransformerUtils.firstOrNull(endpoint.getTransformers()).getName());
    }

    @Test
    public void testResponseTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            "test:///tmp?responseTransformers=NoActionTransformer");
        assertEquals("NoActionTransformer", TransformerUtils.firstOrNull(endpoint.getResponseTransformers()).getName());
    }
}
