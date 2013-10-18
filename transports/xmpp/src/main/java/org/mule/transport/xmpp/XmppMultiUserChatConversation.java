/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.transport.ConnectException;
import org.mule.util.UUID;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class XmppMultiUserChatConversation extends AbstractXmppConversation
{
    private MultiUserChat chat;
    private String nickname;
    private final Connector connector;
    
    public XmppMultiUserChatConversation(ImmutableEndpoint endpoint)
    {
        super(endpoint);
        connector = endpoint.getConnector();
        
        Object nickValue = endpoint.getProperty(XmppConnector.XMPP_NICKNAME);
        if (nickValue != null)
        {
            nickname = nickValue.toString();
        }
        else
        {
            nickname = UUID.getUUID();
        }
    }

    @Override
    protected void doConnect() throws ConnectException
    {
        chat = new MultiUserChat(connection, recipient);
        joinChat();
    }

    protected void joinChat() throws ConnectException
    {
        try
        {
            tryToJoinChat();
        }
        catch (XMPPException e)
        {
            if (roomDoesNotExist(e))
            {
                createRoom();
            }
            else
            {
                throw new ConnectException(e, connector);
            }
        }
    }
    
    protected void tryToJoinChat() throws XMPPException
    {
        DiscussionHistory history = new DiscussionHistory();
        history.setMaxStanzas(0);
        
        // use the same default value that the smack API uses internally
        long joinTimeout = SmackConfiguration.getPacketReplyTimeout();
        
        chat.join(nickname, null, history, joinTimeout);
        if (logger.isDebugEnabled())
        {
            logger.debug("joined groupchat '" + recipient + "'");
        }
    }
    
    protected boolean roomDoesNotExist(XMPPException exception)
    {
        XMPPError error = exception.getXMPPError();
        if ((error.getCode() == 404) &&
            error.getCondition().equals(XMPPError.Condition.recipient_unavailable.toString()))
        {
            return true;
        }
        return false;
    }
    
    protected void createRoom() throws ConnectException
    {
        try
        {
            chat.create(nickname);
            if (logger.isDebugEnabled())
            {
                logger.debug("created and joined groupchat '" + recipient + "'");
            }
        }
        catch (XMPPException e)
        {
            throw new ConnectException(e, connector);
        }
    }

    @Override
    protected void doDisconnect()
    {
        chat.leave();
    }
    
    /**
     * This implementation returns <code>null</code> as we override {@link #receive()} and
     * {@link #receive(long)}.
     */
    @Override
    protected PacketCollector createPacketCollector()
    {
        return null;
    }

    public void dispatch(Message message) throws XMPPException
    {
        message.setType(Message.Type.groupchat);
        message.setTo(recipient);
        
        chat.sendMessage(message);
    }

    @Override
    public Message receive()
    {
        return chat.nextMessage();
    }

    @Override
    public Message receive(long timeout)
    {
        return chat.nextMessage(timeout);
    }
}
