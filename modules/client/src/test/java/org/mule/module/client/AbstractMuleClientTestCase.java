/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.client;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

public abstract class AbstractMuleClientTestCase extends AbstractMuleTestCase
{

    public void testInboundEndpointCache() throws MuleException
    {
        MuleClient muleClient = new MuleClient();
        InboundEndpoint endpointa = muleClient.getInboundEndpoint("test://test1");
        InboundEndpoint endpointd = muleClient.getInboundEndpoint("test://test2");
        InboundEndpoint endpointb = muleClient.getInboundEndpoint("test://test1");
        InboundEndpoint endpointc = muleClient.getInboundEndpoint("test://test1");
        assertEquals(endpointa, endpointc);
        assertEquals(endpointb, endpointb);
        assertNotSame(endpointa, endpointd);
    }

    public void testOutboundEndpointCache() throws MuleException
    {
        MuleClient muleClient = new MuleClient();
        OutboundEndpoint endpointa = muleClient.getOutboundEndpoint("test://test1");
        OutboundEndpoint endpointb = muleClient.getOutboundEndpoint("test://test1");
        OutboundEndpoint endpointd = muleClient.getOutboundEndpoint("test://test2");
        OutboundEndpoint endpointc = muleClient.getOutboundEndpoint("test://test1");
        assertEquals(endpointa, endpointc);
        assertEquals(endpointb, endpointb);
        assertNotSame(endpointa, endpointd);
    }

}
