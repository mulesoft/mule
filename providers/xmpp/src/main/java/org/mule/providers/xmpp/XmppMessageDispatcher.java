/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.xmpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>XmppMessageDispatcher</code> allows Mule events to be sent and
 * received over Xmpp
 * 
 * @author Peter Braswell
 * @author Ross Mason
 * @version $Revision$
 */

public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(XmppMessageDispatcher.class);

    private XmppConnector connector;

    private XMPPConnection xmppConnection = null;

    private Chat chat;

    private GroupChat groupChat;

    public XmppMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XmppConnector) endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        if(xmppConnection==null) {
            UMOEndpointURI uri = endpoint.getEndpointURI();
            xmppConnection = connector.createXmppConnection(uri);
        }
    }

    protected void doDisconnect() throws Exception {
        try {
            if(groupChat!=null) {
            groupChat.leave();
        }
            if(xmppConnection!=null) {
                xmppConnection.close();
            }
        } finally {
            xmppConnection=null;
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
        if(useRemoteSync(event)) {
            Message response=null;
            if(groupChat!=null) {
                response = groupChat.nextMessage(event.getTimeout());
            } else {
                response = chat.nextMessage(event.getTimeout());
            }

            if(response!=null) {
                logger.debug("Got a response from chat: " + chat);
                return new MuleMessage(connector.getMessageAdapter(response));
            }
        }
        return null;
    }

    protected void sendMessage(UMOEvent event) throws Exception {
        if(chat==null && groupChat==null) {
            UMOMessage msg = event.getMessage();
            boolean group = msg.getBooleanProperty(XmppConnector.XMPP_GROUP_CHAT, false);
            String nickname = msg.getStringProperty(XmppConnector.XMPP_NICKNAME, "mule");
            String recipient = event.getEndpoint().getEndpointURI().getPath().substring(1);

            if(group) {
                groupChat = new GroupChat(xmppConnection, recipient);
                if(!groupChat.isJoined()) {
                    groupChat.join(nickname);
                }
            } else {
                chat = new Chat(xmppConnection, recipient);
            }
        }
        Object msgObj = event.getMessage().getPayload();
        Message message;
        // avoid duplicate transformation
        if (!(msgObj instanceof Message)) {
            message = (Message)event.getTransformedMessage();
        } else {
            message = (Message) msgObj;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Transformed packet: " + message.toXML());
        }

        if(chat!=null) {
            chat.sendMessage(message);
        } else {
            groupChat.sendMessage(message);
        }
        if(logger.isDebugEnabled()) {
            logger.debug("packet successfully sent");
        }
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout  the maximum time the operation should block before returning. The call should
     *                 return immediately if there is data available. If no data becomes available before the timeout
     *                 elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception {

        //Should be in the form of xmpp://user:pass@host:[port]/folder
        String to = (String)endpoint.getProperty("folder");
        if(to==null) {
            throw new MalformedEndpointException(endpoint.getEndpointURI().toString());
        }
        Chat chat = xmppConnection.createChat(to);
        Message message = null;
        if(timeout == UMOEvent.TIMEOUT_WAIT_FOREVER) {
            message = chat.nextMessage();
        } else if(timeout == UMOEvent.TIMEOUT_DO_NOT_WAIT) {
            message = chat.nextMessage(1);
        } else {
            message = chat.nextMessage(timeout);
        }
        if(message!=null) {
            return new MuleMessage(connector.getMessageAdapter(message));
        } else {
            return null;
        }
    }

    public Object getDelegateSession()
    {
        return null;
    }
}
