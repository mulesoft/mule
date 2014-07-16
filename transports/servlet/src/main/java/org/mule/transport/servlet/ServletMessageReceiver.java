/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;

/**
 * <code>ServletMessageReceiver</code> is a receiver that is invoked from a Servlet
 * when an event is received. There is a one-to-one mapping between a
 * ServletMessageReceiver and a servlet in the serving webapp.
 */
public class ServletMessageReceiver extends AbstractMessageReceiver
{
    public ServletMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // nothing to do
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }

    @Override
    protected void doStart() throws MuleException
    {
        // nothing to do
    }
}
