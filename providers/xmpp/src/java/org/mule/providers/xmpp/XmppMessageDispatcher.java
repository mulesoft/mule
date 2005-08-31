/*
 * $Header$
 * $Revision$
 * $Date$
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

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>XmppMessageDispatcher</code> allows Mule events to be sent and
 * recieved over Xmpp
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

    private SynchronizedBoolean initialized = new SynchronizedBoolean(false);

    private XMPPConnection xmppConnection = null;

    private Chat chat;

    private GroupChat groupChat;

    public XmppMessageDispatcher(AbstractConnector connector)
    {
        super(connector);
        this.connector = (XmppConnector) connector;
    }

    protected synchronized void initialize(UMOEndpointURI uri) throws InitialisationException
    {
        logger.debug("initialise()");

        if (!initialized.get()) {
            try {
                xmppConnection = connector.findOrCreateXmppConnection(uri);

                if (!xmppConnection.isConnected()) {
                    throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                                                                                       "XMPP Connection",
                                                                                       uri),
                                                      this);

                }
                initialized.set(true);
            } catch (XMPPException e) {
                throw new InitialisationException(e, this);
            }

        }
    }

    public void doDispose()
    {
        if(groupChat!=null) groupChat.leave();
        if (null != xmppConnection) {
            xmppConnection.close();
        }
        initialized.set(false);
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        sendMessage(event);
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        sendMessage(event);
        if(useRemoteSync(event)) {
            Message response=null;
            if(groupChat!=null) {
                response = groupChat.nextMessage(event.getEndpoint().getRemoteSyncTimeout());
            } else {
                response = chat.nextMessage(event.getEndpoint().getRemoteSyncTimeout());
            }

            if(response!=null) {
                logger.debug("Got a response from chat: " + chat);
                return new MuleMessage(connector.getMessageAdapter(response));
            }
        }
        return null;
    }

    protected void sendMessage(UMOEvent event) throws Exception {
        initialize(event.getEndpoint().getEndpointURI());
        if(chat==null && groupChat==null) {
            boolean group = event.getBooleanProperty(XmppConnector.XMPP_GROUP_CHAT, false);
            String nickname = (String)event.getProperty(XmppConnector.XMPP_NICKNAME, "mule");
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
        Message message = (Message)event.getTransformedMessage();

        if (logger.isTraceEnabled()) {
            logger.trace("Transformed packet: " + message.toXML());
        }

        while (!xmppConnection.isConnected() && !initialized.get()) {
            initialize(null);
            Thread.sleep(150);
        }

        if(chat!=null) {
            chat.sendMessage(message);
        } else {
            groupChat.sendMessage(message);
        }
        if(logger.isDebugEnabled()) logger.debug("packet successfully sent");
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {

        while (!xmppConnection.isConnected() && !initialized.get()) {
            initialize(null);
            Thread.sleep(150);
        }
        //Should be in the form of xmpp://user:pass@host:[port]/folder
        String to = (String)endpointUri.getParams().get("folder");
        if(to==null) {
            throw new MalformedEndpointException(endpointUri.toString());
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
