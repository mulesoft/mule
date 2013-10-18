/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;

public final class UnsupportedMessageRequester extends AbstractMessageRequester
{

    public UnsupportedMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    protected MuleMessage doRequest(long timeout) throws Exception
    {
        throw new UnsupportedOperationException("Request not supported for this transport");
    }

    protected void doInitialise()
    {
        // empty
    }

    protected void doDispose()
    {
        // empty
    }

    protected void doConnect() throws Exception
    {
        // empty
    }

    protected void doDisconnect() throws Exception
    {
        // empty
    }

    protected void doStart() 
    {
        // empty
    }

    protected void doStop() 
    {
        // empty
    }
}
