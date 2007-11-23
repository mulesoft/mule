/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

public class VMMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    VMMessageReceiver receiver;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        receiver = new VMMessageReceiver(endpoint.getConnector(), component, endpoint);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
     */
    public UMOMessageReceiver getMessageReceiver()
    {
        return receiver;
    }

    public UMOImmutableEndpoint getEndpoint() throws Exception
    {
        return managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("vm://test");
    }
}
