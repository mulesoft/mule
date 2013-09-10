/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;


public class MessageProcessorPollingConnector extends AbstractConnector
{

    public MessageProcessorPollingConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void doConnect() throws Exception
    {
    }

    @Override
    protected void doDisconnect() throws Exception
    {
    }

    @Override
    protected void doDispose()
    {
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
    }

    @Override
    protected void doStart() throws MuleException
    {
    }

    @Override
    protected void doStop() throws MuleException
    {

    }

    public String getProtocol()
    {
        return "polling";
    }

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        return flowConstruct.getName() + "~" + endpoint.getEndpointURI().getAddress();
    }

}
