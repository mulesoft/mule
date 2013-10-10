/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;

import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule messages to be received over XMPP
 */
public class XmppMessageRequester extends AbstractMessageRequester
{
    private XmppConnector connector;
    private XmppConversation conversation;    

    public XmppMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        connector = (XmppConnector) endpoint.getConnector();
        conversation = connector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        conversation.connect();
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
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        Message message = null;
        if (timeout == MuleEvent.TIMEOUT_WAIT_FOREVER)
        {
            message = conversation.receive();
        }
        else
        {
            message = conversation.receive(timeout);
        }
        
        if (message != null)
        {
            return createMuleMessage(message, endpoint.getEncoding());
        }
        else
        {
            return null;
        }
    }
}
