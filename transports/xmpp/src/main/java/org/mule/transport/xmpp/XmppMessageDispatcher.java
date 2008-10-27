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
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.AbstractMessageDispatcher;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule events to be sent over Xmpp
 */

public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    private final XmppConnector connector;
    private volatile XMPPConnection xmppConnection = null;
    private volatile Chat chat;
    private volatile GroupChat groupChat;

    public XmppMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XmppConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (xmppConnection == null)
        {
            EndpointURI uri = endpoint.getEndpointURI();
            xmppConnection = connector.createXmppConnection(uri);
        }
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            if (groupChat != null)
            {
                groupChat.leave();
            }
            if (xmppConnection != null)
            {
                xmppConnection.close();
            }
        }
        finally
        {
            xmppConnection = null;
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        sendMessage(event);
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        sendMessage(event);

        if (returnResponse(event))
        {
            Message response;

            if (groupChat != null)
            {
                response = groupChat.nextMessage(event.getTimeout());
            }
            else
            {
                response = chat.nextMessage(event.getTimeout());
            }

            if (response != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Got a response from chat: " + chat);
                }
                return new DefaultMuleMessage(connector.getMessageAdapter(response));
            }
        }
        return null;
    }

    protected void sendMessage(MuleEvent event) throws Exception
    {
        if (chat == null && groupChat == null)
        {
            MuleMessage msg = event.getMessage();
            boolean group = msg.getBooleanProperty(XmppConnector.XMPP_GROUP_CHAT, false);
            String nickname = msg.getStringProperty(XmppConnector.XMPP_NICKNAME, "mule");
            String recipient = event.getEndpoint().getEndpointURI().getPath().substring(1);

            if (group)
            {
                groupChat = new GroupChat(xmppConnection, recipient);
                if (!groupChat.isJoined())
                {
                    groupChat.join(nickname);
                }
            }
            else
            {
                chat = new Chat(xmppConnection, recipient);
            }
        }

        Object msgObj = event.getMessage().getPayload();
        Message message;
        // avoid duplicate transformation
        if (!(msgObj instanceof Message))
        {
            message = (Message)event.transformMessage();
        }
        else
        {
            message = (Message)msgObj;
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("Transformed packet: " + message.toXML());
        }

        // if the endpoint specified a designated recipient, use that
        if (message.getTo() != null)
        {
            xmppConnection.sendPacket(message);
        }
        else if (chat != null)
        {
            chat.sendMessage(message);
        }
        else
        {
            groupChat.sendMessage(message);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Packet successfully sent");
        }
    }

}
