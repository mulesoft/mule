/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.NoActionTransformer;
import org.mule.transformer.TransformerUtils;

public class EndpointTransformerTestCase extends AbstractMuleTestCase
{

    public void testTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "test:///tmp?transformers=NoActionTransformer");
        assertEquals("NoActionTransformer", TransformerUtils.firstOrNull(endpoint.getTransformers()).getName());
    }

    public void testResponseTransformerProperty() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new NoActionTransformer());
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test:///tmp?responseTransformers=NoActionTransformer");
        assertEquals("NoActionTransformer", TransformerUtils.firstOrNull(endpoint.getResponseTransformers()).getName());
    }
}
