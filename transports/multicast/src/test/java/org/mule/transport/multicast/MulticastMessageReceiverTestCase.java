/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.multicast;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiverTestCase;

import com.mockobjects.dynamic.Mock;

public class MulticastMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(Service.class);
        mockComponent.expect("getResponseRouter");
        mockComponent.expectAndReturn("getInboundRouter", new ServiceCompositeMessageSource());

        return new MulticastMessageReceiver((AbstractConnector)endpoint.getConnector(),
            (Service)mockComponent.proxy(), endpoint);
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("multicast://228.2.3.4:10100");
    }
}
