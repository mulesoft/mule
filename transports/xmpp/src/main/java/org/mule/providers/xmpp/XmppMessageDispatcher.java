/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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

    public XmppMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XmppConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (xmppConnection == null)
        {
            UMOEndpointURI uri = endpoint.getEndpointURI();
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

    protected void doDispatch(UMOEvent event) throws Exception
    {
        sendMessage(event);
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        sendMessage(event);

        if (useRemoteSync(event))
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
                return new MuleMessage(connector.getMessageAdapter(response));
            }
        }
        return null;
    }

    protected void sendMessage(UMOEvent event) throws Exception
    {
        if (chat == null && groupChat == null)
        {
            UMOMessage msg = event.getMessage();
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
            message = (Message)event.getTransformedMessage();
        }
        else
        {
            message = (Message)msgObj;
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("Transformed packet: " + message.toXML());
        }

        if (chat != null)
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
