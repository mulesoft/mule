/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;

public final class UnsupportedMessageDispatcher extends AbstractMessageDispatcher
{

    public UnsupportedMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Dispatch not supported for this transport.");
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Send not supported for this transport.");
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
