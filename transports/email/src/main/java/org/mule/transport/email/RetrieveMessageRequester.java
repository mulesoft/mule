/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;

import com.sun.mail.imap.IMAPMessage;

import java.net.URLDecoder;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

/**
 * This dispatcher can only be used to receive message (as opposed to listening for them).
 * Trying to send or dispatch will throw an UnsupportedOperationException.
 * <p/>
 * This contains a reference to a mail folder (and also the endpoint and connector, via superclasses)
 */

public class RetrieveMessageRequester extends AbstractMessageRequester
{
    private Folder folder;
    private Folder moveToFolder;

    public RetrieveMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    private AbstractRetrieveMailConnector castConnector()
    {
        return (AbstractRetrieveMailConnector) getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (folder == null || !folder.isOpen())
        {

            Store store = castConnector().getSessionDetails(endpoint).newStore();

            EndpointURI uri = endpoint.getEndpointURI();
            String user = (uri.getUser() != null ? URLDecoder.decode(uri.getUser()) : null);
            String pass = (uri.getPassword() != null ? URLDecoder.decode(uri.getPassword()) : null);
            store.connect(uri.getHost(), uri.getPort(), user, pass);

            folder = store.getFolder(castConnector().getMailboxFolder());
            if (!folder.isOpen())
            {
                try
                {
                    // Depending on Server implementation it's not always
                    // necessary to open the folder to check it
                    // Opening folders can be exprensive!
                    folder.open(Folder.READ_WRITE);
                }
                catch (MessagingException e)
                {
                    logger.warn("Failed to open folder: " + folder.getFullName() + " This is not an exception since some server implementations do not require the flder to be open", e);
                }
            }

            if (castConnector().getMoveToFolder() != null)
            {
                moveToFolder = store.getFolder(castConnector().getMoveToFolder());
                if (!moveToFolder.isOpen())
                {
                    try
                    {
                        // Depending on Server implementation it's not always
                        // necessary to open the folder to check it
                        // Opening folders can be exprensive!
                        moveToFolder.open(Folder.READ_WRITE);
                    }
                    catch (MessagingException e)
                    {
                        logger.warn("Failed to open folder: " + moveToFolder.getFullName() + " This is not an exception since some server implementations do not require the flder to be open", e);
                    }
                }
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        // close and expunge deleted messages
        try
        {
            if (folder != null)
            {
                try
                {
                    folder.expunge();
                }
                catch (MessagingException e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("ignoring exception on expunge: " + e.getMessage());
                    }
                }
                if (folder.isOpen())
                {
                    folder.close(true);
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to close inbox: " + e.getMessage(), e);
        }

        try
        {
            if (moveToFolder != null)
            {
                if (moveToFolder.isOpen())
                {
                    moveToFolder.close(false);
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to close moveToFolder: " + e.getMessage(), e);
        }
    }

    /**
     * @param event
     * @throws UnsupportedOperationException
     */
    protected void doDispatch(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot dispatch from a Pop3 connection");
    }

    /**
     * @param event
     * @throws UnsupportedOperationException
     */
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Cannot send from a Pop3 connection");
    }

    /**
     * Make a specific request to the underlying transport. Endpoint can be in the
     * form of pop3://username:password@pop3.lotsofmail.org
     *
     * @param timeout the maximum time the operation should block before returning.
     *                The call should return immediately if there is data available. If
     *                no data becomes available before the timeout elapses, null will be
     *                returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal causes an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        long t0 = System.currentTimeMillis();
        if (timeout < 0)
        {
            timeout = Long.MAX_VALUE;
        }

        do
        {
            if (hasMessages(folder))
            {
                int count = getMessageCount(folder);
                if (count > 0)
                {
                    Message message = getNextMessage(folder);
                    // so we don't get the same message again
                    flagMessage(folder, message);

                    if (moveToFolder != null)
                    {
                        Message newMessage = message;
                        //If we're using IMAP we need to cache the message contents so the message is accessible after the
                        //folder is closed
                        if(message instanceof IMAPMessage)
                        {
                            //We need to copy and cache so that the message cna be moved
                            newMessage = new MimeMessage((IMAPMessage)message);
                        }
                        folder.copyMessages(new Message[]{message}, moveToFolder);
                        message = newMessage;
                    }
                    return new DefaultMuleMessage(castConnector().getMessageAdapter(message), connector.getMuleContext());
                }
                else if (count == -1)
                {
                    throw new MessagingException("Cannot monitor folder: " + folder.getFullName()
                            + " as folder is closed");
                }
            }

            long sleep =
                    Math.min(castConnector().getCheckFrequency(),
                            timeout - (System.currentTimeMillis() - t0));

            if (sleep > 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("No results, sleeping for " + sleep);
                }
                Thread.sleep(sleep);
            }
            else
            {

                logger.debug("Timeout");
                return null;
            }

        }
        while (true);
    }

    /**
     * There seems to be som variation on pop3 implementation so it may be
     * preferrable to mark messages as seen here and alos overload the getMessages
     * method to grab only new messages
     *
     * @param message
     * @throws javax.mail.MessagingException
     */
    protected void flagMessage(Folder folder, Message message) throws MessagingException
    {
        if (castConnector().isDeleteReadMessages())
        {
            message.setFlag(Flags.Flag.DELETED, true);
        }
        else
        {
            message.setFlag(Flags.Flag.SEEN, true);
        }
    }

    protected Message getNextMessage(Folder folder) throws MessagingException
    {
        if(getMessageCount(folder) > 0)
        {
            return folder.getMessage(1);
        }
        else
        {
            return null;
        }
    }

    protected int getMessageCount(Folder folder) throws MessagingException
    {
        return folder.getMessageCount();
    }

    /**
     * Optimised check to se whether to return the message count and retrieve the
     * messages. Some pop3 implementations differ so an optimised check such as
     * folder.hasNewMessages() cannot be used
     *
     * @param folder
     * @throws javax.mail.MessagingException
     */
    protected boolean hasMessages(Folder folder) throws MessagingException
    {
        return getMessageCount(folder) > 0;
    }

    protected void doDispose()
    {
        if (null != folder && folder.isOpen())
        {
            try
            {

                folder.close(true);
            }
            catch (Exception e)
            {
                logger.debug("ignoring exception: " + e.getMessage(), e);
            }
        }
    }

}