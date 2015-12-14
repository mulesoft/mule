/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    private XmppConversation conversation;

    public XmppMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        XmppConnector xmppConnector = (XmppConnector) endpoint.getConnector();
        conversation = xmppConnector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        conversation.connect(true);
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
