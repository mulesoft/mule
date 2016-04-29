/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm;

import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.transport.AbstractMessageReceiverTestCase;

public class VMMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    VMMessageReceiver receiver;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        receiver = new VMMessageReceiver(endpoint.getConnector(), flow, endpoint);
    }

    @Override
    public MessageReceiver getMessageReceiver()
    {
        return receiver;
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("vm://test");
    }
}
