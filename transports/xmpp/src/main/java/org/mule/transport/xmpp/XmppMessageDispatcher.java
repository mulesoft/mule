/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule events to be sent over Xmpp
 */
public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    private final XmppConnector xmppConnector;
    private XmppConversation conversation;

    public XmppMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        xmppConnector = (XmppConnector) endpoint.getConnector();
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
//                return createMuleMessage(response);
//            }
//        }
        return new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
    }

    protected void sendMessage(MuleEvent event) throws Exception
    {
        // Handle session closing by the server
        if (!xmppConnector.getXmppConnection().isConnected())
        {
            xmppConnector.getXmppConnection().connect();
        }

        Message jabberMessage = event.getMessage().getPayload(DataTypeFactory.create(Message.class));
        conversation.dispatch(jabberMessage);

        if (logger.isDebugEnabled())
        {
            String recipient = XmppConnector.getRecipient(endpoint);
            logger.debug("Message \"" + jabberMessage.getBody()
                + "\" successfully sent to " + recipient);
        }
    }
}
