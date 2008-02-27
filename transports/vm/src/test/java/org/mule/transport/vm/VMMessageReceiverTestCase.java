/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractMessageReceiverTestCase;

public class VMMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    VMMessageReceiver receiver;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        receiver = new VMMessageReceiver(endpoint.getConnector(), service, endpoint);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
     */
    public MessageReceiver getMessageReceiver()
    {
        return receiver;
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("vm://test");
    }
}
