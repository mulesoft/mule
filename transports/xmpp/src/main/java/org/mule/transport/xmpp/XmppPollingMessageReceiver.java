/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;

import org.jivesoftware.smack.packet.Message;

public class XmppPollingMessageReceiver extends AbstractPollingMessageReceiver
{
    private XmppConversation conversation;

    public XmppPollingMessageReceiver(Connector conn, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(conn, flowConstruct, endpoint);
        XmppConnector xmppConnector = (XmppConnector) conn;
        conversation = xmppConnector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        conversation.connect(false);
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        conversation.disconnect();
    }

    @Override
    protected void doDispose()
    {
        conversation = null;
    }

    @Override
    public void poll() throws Exception
    {
        // Wait 10% less than the polling frequency. This approach makes sure that we finish
        // in time before the next poll call comes in
        try
        {
            long frequency = getFrequency();
            long tenPercent = (long)(frequency * 0.1);
            long pollTimeout = frequency - tenPercent;

            Message xmppMessage = conversation.receive(pollTimeout);
            if (xmppMessage == null)
            {
                return;
            }

            processMessage(xmppMessage);
        }
        catch (Exception e)
        {
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            throw e;
        }
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }

    protected void processMessage(final Message xmppMessage) throws MuleException
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        try
        {
            executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    MuleMessage muleMessage = createMuleMessage(xmppMessage);
                    return routeMessage(muleMessage);
                }
            });
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }
}
