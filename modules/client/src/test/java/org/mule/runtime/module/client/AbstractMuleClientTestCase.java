/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public abstract class AbstractMuleClientTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testInboundEndpointCache() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        InboundEndpoint endpointa = muleClient.getInboundEndpoint("test://test1");
        InboundEndpoint endpointd = muleClient.getInboundEndpoint("test://test2");
        InboundEndpoint endpointb = muleClient.getInboundEndpoint("test://test1");
        InboundEndpoint endpointc = muleClient.getInboundEndpoint("test://test1");
        assertEquals(endpointa, endpointc);
        assertEquals(endpointb, endpointb);
        assertNotSame(endpointa, endpointd);
    }

    @Test
    public void testOutboundEndpointCache() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        OutboundEndpoint endpointa = muleClient.getOutboundEndpoint("test://test1", 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        OutboundEndpoint endpointb = muleClient.getOutboundEndpoint("test://test1", 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        OutboundEndpoint endpointd = muleClient.getOutboundEndpoint("test://test2", 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        OutboundEndpoint endpointc = muleClient.getOutboundEndpoint("test://test1", 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        assertEquals(endpointa, endpointc);
        assertEquals(endpointb, endpointb);
        assertNotSame(endpointa, endpointd);
    }

}
