/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.transport.udp.UdpConnector;
import org.mule.transport.udp.UdpMessageReceiver;

import com.mockobjects.dynamic.Mock;

public class UdpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    public MessageReceiver getMessageReceiver() throws Exception
    {
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "udp://localhost:10100");
        Mock mockComponent = new Mock(Service.class);
        mockComponent.expectAndReturn("getResponseTransformer", null);
        mockComponent.expectAndReturn("getResponseRouter", null);

        return new UdpMessageReceiver(endpoint.getConnector(), (Service) mockComponent.proxy(), endpoint);
    }

    public ImmutableEndpoint getEndpoint() throws Exception
    {
        EndpointBuilder builder = new EndpointURIEndpointBuilder("udp://localhost:10100", muleContext);
        builder.setConnector(new UdpConnector());
        return muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(builder);
    }

}
