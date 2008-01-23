/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.multicast;

import org.mule.api.component.Component;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.transport.AbstractConnector;
import org.mule.transport.multicast.MulticastMessageReceiver;

import com.mockobjects.dynamic.Mock;

public class MulticastMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(Component.class);
        mockComponent.expectAndReturn("getResponseTransformer", null);
        mockComponent.expectAndReturn("getResponseRouter", null);

        return new MulticastMessageReceiver((AbstractConnector)endpoint.getConnector(),
            (Component)mockComponent.proxy(), endpoint);
    }

    public ImmutableEndpoint getEndpoint() throws Exception
    {
        return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("multicast://228.2.3.4:10100");
    }
}
