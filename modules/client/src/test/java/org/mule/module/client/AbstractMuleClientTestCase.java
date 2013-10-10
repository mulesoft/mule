/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

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
