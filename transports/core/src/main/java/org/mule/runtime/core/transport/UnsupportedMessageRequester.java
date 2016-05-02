/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;

public final class UnsupportedMessageRequester extends AbstractMessageRequester
{

    public UnsupportedMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        throw new UnsupportedOperationException("Request not supported for this transport");
    }

    @Override
    protected void doInitialise()
    {
        // empty
    }

    @Override
    protected void doDispose()
    {
        // empty
    }

    @Override
    protected void doConnect() throws Exception
    {
        // empty
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // empty
    }

    @Override
    protected void doStart() 
    {
        // empty
    }

    @Override
    protected void doStop() 
    {
        // empty
    }
}
