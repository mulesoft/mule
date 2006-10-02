/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

/**
 * <code>Pop3MessageDispatcher</code> For Pop3 connections the dispatcher can
 * only be used to receive message (as opposed to listening for them). Trying to
 * send or dispatch will throw an UnsupportedOperationException.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Pop3MessageDispatcher extends AbstractMessageDispatcher
{
    private Pop3Connector connector;

    private Folder folder;

    private Session session = null;

    public Pop3MessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (Pop3Connector)endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception {

        if (folder == null || !folder.isOpen()) {
            String inbox = (String)endpoint.getProperty("folder");

            if(inbox == null || endpoint.getProtocol().toLowerCase().startsWith("pop3"))  {
                inbox = Pop3Connector.MAILBOX;
            }

            URLName url = new URLName(endpoint.getEndpointURI().getScheme(),
                                      endpoint.getEndpointURI().getHost(),
                                      endpoint.getEndpointURI().getPort(),
                                      inbox,
                                      endpoint.getEndpointURI().getUsername(),
                                      endpoint.getEndpointURI().getPassword());

            session = MailUtils.createMailSession(url, connector);
            session.setDebug(logger.isDebugEnabled());

            Store store = session.getStore(url);
            store.connect(endpoint.getEndpointURI().getHost(), endpoint.getEndpointURI().getPort(),
                    endpoint.getEndpointURI().getUsername(), endpoint.getEndpointURI().getPassword());
            folder = store.getFolder(inbox);
            if (!folder.isOpen()) {
                try {
                    // Depending on Server implementation it's not always
                    // necessary to open the folder to check it
                    // Opening folders can be exprensive!
                    // folder.open(Folder.READ_ONLY);
                    folder.open(Folder.READ_WRITE);
                } catch (MessagingException e) {
                    logger.warn("Failed to open folder: " + folder.getFullName(), e);
                }
            }
        }
    }

    protected void doDisconnect() throws Exception {
        // close and expunge deleted messages
        try {
            if (folder != null) {
                try {
                    folder.expunge();
                } catch (MessagingException e) {
                    // maybe next time.
                }
                 if(folder.isOpen()) {
                     folder.close(true);
                 }
            }
            session = null;
        } catch (Exception e) {
            logger.error("Failed to close inbox: " + e.getMessage(), e);
        }
    }

    /**
     * 
     * @param event
     * @throws UnsupportedOperationException
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot dispatch from a Pop3 connection");
    }

    /**
     * 
     * @param event
     * @return
     * @throws UnsupportedOperationException
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot send from a Pop3 connection");
    }

    /**
     * Make a specific request to the underlying transport
     * Endpoint can be in the form of pop3://username:password@pop3.lotsofmail.org
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

        long t0 = System.currentTimeMillis();
        if (timeout < 0) {
            timeout = Long.MAX_VALUE;
        }
        
        do {
            if(hasMessages(folder)) {
                int count = getMessageCount(folder);
                if (count > 0) {
                    Message message = getNextMessage(folder);
                    // so we don't get the same message again
                    flagMessage(folder, message);

                    return new MuleMessage(connector.getMessageAdapter(message));
                } else if (count == -1) {
                    throw new MessagingException("Cannot monitor folder: " + folder.getFullName() + " as folder is closed");
                }
            }
            long sleep = Math.min(this.connector.getCheckFrequency(), timeout - (System.currentTimeMillis() - t0));
            if (sleep > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No results, sleeping for " + sleep);
                }
                Thread.sleep(sleep);
            } else {
                logger.debug("Timeout");
                return null;
            }

        } while (true);        
    }

    /**
     * There seems to be som variation on pop3 implementation so it may be preferrable to mark messages as seen here and
     * alos overload the getMessages method to grab only new messages
     * @param message
     * @throws MessagingException
     */
    protected void flagMessage(Folder folder, Message message) throws MessagingException {
        message.setFlag(Flags.Flag.DELETED, true);
    }

    protected Message getNextMessage(Folder folder) throws MessagingException {
        return folder.getMessage(1);
    }

    protected int getMessageCount(Folder folder) throws MessagingException {
        return folder.getMessageCount();
    }

    /**
     * Optimised check to se whether to return the message count and retrieve the messages. Some pop3 implementations differ
     * so an optimised check such as folder.hasNewMessages() cannot be used
     * @param folder
     * @return
     * @throws MessagingException
     */
    protected boolean hasMessages(Folder folder) throws MessagingException {
        return getMessageCount(folder) > 0;
    }

    public Object getDelegateSession() throws UMOException
    {
        return session;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    protected void doDispose()
    {
        // template method
    }
}
