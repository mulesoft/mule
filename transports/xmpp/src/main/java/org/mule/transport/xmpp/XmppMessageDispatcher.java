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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule events to be sent over Xmpp
 */
public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    private final XmppConnector connector;
    private XmppConversation conversation;

    public XmppMessageDispatcher(OutboundEndpoint endpoint)
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
    protected void doDispatch(MuleEvent event) throws Exception
    {
        sendMessage(event);
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        sendMessage(event);

        // TODO xmpp: even sync endpoints do not wait for a reply. Look at how the JMS transport handles replies, use reply handler
//        if (returnResponse(event, false))
//        {
//            Message response = conversation.receive(event.getTimeout());
//
////            if (groupChat != null)
////            {
////                response = groupChat.nextMessage(event.getTimeout());
////            }
//
//            if (response != null)
//            {
//                return new DefaultMuleMessage(connector.getMessageAdapter(response), 
//                    connector.getMuleContext());
//            }
//        }
        return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
    }

    protected void sendMessage(MuleEvent event) throws Exception
    {
        Object payload = event.getMessage().getPayload();
        
        Message jabberMessage = null;
        if (payload instanceof Message)
        {
            jabberMessage = (Message) payload;
        }
        else
        {
            jabberMessage = (Message) event.transformMessage();
        }

        conversation.dispatch(jabberMessage);
        
        if (logger.isDebugEnabled())
        {
            String recipient = XmppConnector.getRecipient(endpoint);
            logger.debug("Message \"" + jabberMessage.getBody() 
                + "\" successfully sent to " + recipient);
        }
    }
}
