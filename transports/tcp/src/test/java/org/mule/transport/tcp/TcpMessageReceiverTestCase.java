/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiverTestCase;

import com.mockobjects.dynamic.Mock;

public class TcpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(Service.class);
        mockComponent.expect("getResponseRouter");
        mockComponent.expectAndReturn("getInboundRouter", new ServiceCompositeMessageSource());
        return new TcpMessageReceiver((AbstractConnector)endpoint.getConnector(),
            (Service)mockComponent.proxy(), endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("tcp://localhost:1234");
    }
}
