/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
