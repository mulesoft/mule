/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.multicast;

import static org.mockito.Mockito.mock;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiverTestCase;

public class MulticastMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Service mockService = mock(Service.class);
        AbstractConnector connector = (AbstractConnector)endpoint.getConnector();
        return new MulticastMessageReceiver(connector, mockService, endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("multicast://228.2.3.4:10100");
    }
}
