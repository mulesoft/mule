/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class JabberClient implements PacketListener, MessageListener
{
    private static Log logger = LogFactory.getLog(JabberClient.class);
    
    private String host;
    private String user;
    private String password;
    private boolean synchronous = true;
    private String replyPayload = "Reply";
    private boolean autoreply = false;

    private XMPPConnection connection;
    private Map<String, Chat> chats;
    private MultiUserChat groupchat = null;
    private List<Message> replies;
    private PacketCollector packetCollector = null;
    private CountDownLatch messageLatch = null;

    public JabberClient(String host, String user, String password) throws Exception
    {
        super();
        this.host = host;
        
        int index = user.indexOf("@");
        if (index > -1)
        {
            this.user = user.substring(0, index);
        }
        else
        {
            this.user = user;
        }
        
        this.password = password;        

        replies = new ArrayList<Message>();
        chats = new HashMap<String, Chat>();
    }

    public void connect(CountDownLatch latch) throws Exception
    {
        ConnectionConfiguration connectionConfig = new ConnectionConfiguration(host);
        // no roster required
        connectionConfig.setRosterLoadedAtLogin(false);
        
        connection = new XMPPConnection(connectionConfig);
        connection.connect();
        if (logger.isDebugEnabled())
        {
            logger.debug("connected to " + host);
        }
        
        connection.login(user, password);
        if (logger.isDebugEnabled())
        {
            logger.debug("logged into " + host + " as " + user);
        }

        registerListener();
        
        // notify the caller that we're finished connecting
        latch.countDown();
    }

    private void registerListener()
    {
        PacketFilter normalTypeFilter = new MessageTypeFilter(Message.Type.normal);
        PacketFilter chatTypeFilter = new MessageTypeFilter(Message.Type.chat);
        PacketFilter mucTypeFilter = new MessageTypeFilter(Message.Type.groupchat);
        PacketFilter filter = new OrFilter(normalTypeFilter, chatTypeFilter);
        filter = new OrFilter(filter, mucTypeFilter);

        if (synchronous)
        {
            packetCollector = connection.createPacketCollector(filter);
        }
        else
        {
            connection.addPacketListener(this, filter);
        }
    }

    public void disconnect()
    {
        connection.removePacketListener(this);
        
        if (packetCollector != null)
        {
            packetCollector.cancel();
        }
        if (groupchat != null)
        {
            groupchat.leave();
        }
        
        chats = null;
        connection.disconnect();
    }

    //
    // Jabber listeners
    //
    public void processPacket(Packet packet)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("received " + packet);
        }
        
        // our filters make sure that we ever only see Message instances
        replies.add((Message) packet);
        
        countDownMessageLatch();        
        sendAutoreply(packet);
    }

    public void processMessage(Chat chat, Message message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("received from chat '" + chat.getThreadID() + ": " + message);
        }
        
        replies.add(message);
        countDownMessageLatch();
    }

    private void countDownMessageLatch()
    {
        if (messageLatch != null)
        {
            messageLatch.countDown();
        }
    }

    private void sendAutoreply(Packet packet)
    {
        if (autoreply)
        {
            Message incomingMessage = (Message) packet;
            
            Message message = new Message();
            message.setType(incomingMessage.getType());
            message.setTo(incomingMessage.getFrom());
            message.setBody(replyPayload);
            
            connection.sendPacket(message);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("sent autoreply message with payload: \"" + replyPayload + "\"");
            }
        }
    }

    public void sendMessage(String recipient, String payload)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Will send message to \"" + recipient + "\" with payload \"" + payload + "\"");
        }
        
        Message message = buildMessage(Message.Type.normal, recipient, payload);
        connection.sendPacket(message);
    }

    public void sendChatMessage(String recipient, String payload) throws XMPPException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Will send chat message to \"" + recipient + "\" with payload \"" + payload + "\"");
        }

        Chat chat = chatWith(recipient);
        Message message = buildMessage(Message.Type.chat, recipient, payload);
        chat.sendMessage(message);
    }
    
    private Chat chatWith(String recipient)
    {
        Chat chat = chats.get(recipient);
        if (chat == null)
        {
            chat = connection.getChatManager().createChat(recipient, this);
            chats.put(recipient, chat);
        }
        return chat;
    }
    
    private Message buildMessage(Message.Type type, String recipient, String payload)
    {
        Message message = new Message();
        message.setType(type);
        
        String from = user + "@" + host;
        message.setFrom(from);
        
        message.setTo(recipient);
        message.setBody(payload);

        return message;
    }
    
    public List<Message> getReceivedMessages()
    {
        return replies;
    }
    
    public Packet receive(long timeout)
    {
        return packetCollector.nextResult(timeout);
    }

    public void joinGroupchat(String chatroom) throws XMPPException
    {
        groupchat = new MultiUserChat(connection, chatroom);
        groupchat.join(UUID.getUUID().toString());
    }

    public void sendGroupchatMessage(String text) throws XMPPException
    {
        groupchat.sendMessage(text);
    }

    //
    // setters for config parameters
    // 
    public void setReplyPayload(String reply)
    {
        replyPayload = reply;
    }

    public void setAutoReply(boolean flag)
    {
        autoreply = flag;
        // autoreply only works in an async mode
        synchronous = false;
    }
    
    public void setSynchronous(boolean flag)
    {
        synchronous = flag;
    }

    public void setMessageLatch(CountDownLatch latch)
    {
        messageLatch = latch;
    }
}
