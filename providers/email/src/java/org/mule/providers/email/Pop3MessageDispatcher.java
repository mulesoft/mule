/**
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
package org.mule.providers.email;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.mail.*;
import java.util.Properties;

/**
 * <code>Pop3MessageDispatcher</code> For Pop3 connections the dispatcher can only be used
 * to receive message (as opposed  to listening for them). Trying to send or dispatch will
 * throw an UnsupportedOperationException.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Pop3MessageDispatcher extends AbstractMessageDispatcher
{
    private Pop3Connector connector;

    private Folder inbox;

    private SynchronizedBoolean initialised = new SynchronizedBoolean(false);


    public Pop3MessageDispatcher(Pop3Connector connector)
    {
        super(connector);
        this.connector = connector;
    }

    protected void initialise(String endpoint) throws MessagingException
    {

        if(!initialised.get()) {
            URLName url = new URLName(endpoint);

            Properties props = System.getProperties();
            props.put("mail.smtp.host", url.getHost());
            props.put("mail.smtp.port", String.valueOf(url.getPort()));
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(logger.isDebugEnabled());
            PasswordAuthentication pw = new PasswordAuthentication(url.getUsername(), url.getPassword());
            session.setPasswordAuthentication(url, pw);

            Store store = session.getStore(url);
            store.connect();
            //Will always be INBOX for pop3
            inbox = store.getFolder(connector.getMailBox());
            if (!inbox.isOpen()) inbox.open(Folder.READ_ONLY);
        }
    }

    /**
     *
     * @param event
     * @throws UnsupportedOperationException
     */
    public void doDispatch(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot dispatch from a Pop3 connection");
    }
    /**
     *
     * @param event
     * @return
     * @throws UnsupportedOperationException
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot send from a Pop3 connection");
    }

    /**
     * Endpoint can be in the form of
     * pop3://username:password@pop3.muleumo.org
     *
     * @param endpointUri
     * @param timeout
     * @return
     * @throws Exception
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        initialise(endpointUri.getAddress());

            int count = inbox.getMessageCount();
            if (count > 0)
            {
                Message[] message = inbox.getMessages();
                return new MuleMessage(connector.getMessageAdapter(message[0]));
            }
            else if (count == -1)
            {
                throw new MessagingException("Cannot monitor folder: " + inbox.getFullName() + " as folder is closed");
            } else {
                Thread.sleep(timeout);
                count = inbox.getMessageCount();
                if(count > 0) {
                    Message message = inbox.getMessage(0);
                    //so we don't get the same message again
                    message.setFlag(Flags.Flag.DELETED, true);
                    return new MuleMessage(connector.getMessageAdapter(message));
                } else {
                    return null;
                }
            }
    }

    public Object getDelegateSession() throws UMOException
    {
        return connector.getSession();
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
        initialised.set(false);
        //close and expunge deleted messages
        try
        {
            if(inbox!=null) inbox.close(true);
        } catch (MessagingException e)
        {
            logger.error("Failed to close pop3 inbox: " + e.getMessage(), e);
        }
    }
}
