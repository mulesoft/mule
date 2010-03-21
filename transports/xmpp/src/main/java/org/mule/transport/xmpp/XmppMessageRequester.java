/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
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
            return new DefaultMuleMessage(connector.getMessageAdapter(message), connector.getMuleContext());
        }
        else
        {
            return null;
        }
    }
}