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

import org.mule.RegistryContext;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.RoutingException;
import org.mule.util.FileUtils;
import org.mule.util.UUID;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;

/**
 * Poll a mailbox for messages, remove the messages and route them as events into Mule.
 *
 * This contains a reference to a mail folder (and also the endpoint and connector, via superclasses)
 */

public class RetrieveMessageReceiver extends AbstractPollingMessageReceiver
implements MessageCountListener, Startable, Stoppable
{
    private Folder folder = null;
    private String backupFolder = null;

    public RetrieveMessageReceiver(UMOConnector connector,
                                   UMOComponent component,
                                   UMOEndpoint endpoint,
                                   long checkFrequency,
                                   String backupFolder) 
    throws InitialisationException
    {
        super(connector, component, endpoint, checkFrequency);
        this.backupFolder = backupFolder;
        this.connector = (AbstractRetrieveMailConnector) connector;
    }

    private AbstractRetrieveMailConnector castConnector()
    {
        return (AbstractRetrieveMailConnector) getConnector();
    }

    protected void doConnect() throws Exception
    {
        SessionDetails session = castConnector().getSessionDetails(endpoint);

        Store store = session.newStore();
        store.connect();
        folder = store.getFolder(castConnector().getMailboxFolder());

        // If user explicitly sets backup folder to "" it will disable email back up
        if (backupFolder == null)
        {
            this.backupFolder = 
                RegistryContext.getConfiguration().getWorkingDirectory() + "/mail/" + folder.getName();
        }
        else if (StringUtils.EMPTY.equals(backupFolder))
        {
            backupFolder = null;
        }

        if (backupFolder != null && !this.backupFolder.endsWith(File.separator))
        {
            this.backupFolder += File.separator;
        }
    }

    protected void doDisconnect() throws Exception
    {
        // nothing to do here
    }

    protected void doStop()
    {
        if (folder != null)
        {
            folder.removeMessageCountListener(this);
        }
    }

    protected void doStart() throws UMOException
    {
        super.doStart();
        folder.addMessageCountListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.mail.event.MessageCountListener#messagesAdded(javax.mail.event.MessageCountEvent)
     */
    public void messagesAdded(MessageCountEvent event)
    {
        Message messages[] = event.getMessages();
        if (messages != null)
        {
            UMOMessage message = null;
            for (int i = 0; i < messages.length; i++)
            {
                try
                {
                    if (!messages[i].getFlags().contains(Flags.Flag.DELETED))
                    {
                        MimeMessage mimeMessage = new MimeMessage((MimeMessage) messages[i]);
                        storeMessage(mimeMessage);
                        message = new MuleMessage(castConnector().getMessageAdapter(mimeMessage));

                        if (castConnector().isDeleteReadMessages())
                        {
                            // Mark as deleted
                            messages[i].setFlag(Flags.Flag.DELETED, true);
                        }
                        else
                        {
                            messages[i].setFlag(Flags.Flag.SEEN, true);
                        }
                        routeMessage(message, endpoint.isSynchronous());
                    }
                }
                catch (UMOException e)
                {
                    handleException(e);
                }
                catch (Exception e)
                {
                    Exception forwarded;

                    if (message != null)
                    {
                        forwarded = new RoutingException(new org.mule.config.i18n.Message(
                            Messages.ROUTING_ERROR), message, endpoint, e);
                    }
                    else
                    {
                        forwarded = new ReceiveException(endpoint, -1, e);
                    }

                    handleException(forwarded);
                }
            }
        }
    }

    protected UMOMessage handleUnacceptedFilter(UMOMessage message)
    {
        super.handleUnacceptedFilter(message);
        if (message.getPayload() instanceof Message)
        {
            Message msg = (Message) message.getPayload();
            try
            {
                msg.setFlag(Flags.Flag.DELETED, endpoint.isDeleteUnacceptedMessages());
            }
            catch (MessagingException e)
            {
                logger.error("failled to set message deleted: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.mail.event.MessageCountListener#messagesRemoved(javax.mail.event.MessageCountEvent)
     */
    public void messagesRemoved(MessageCountEvent event)
    {
        if (logger.isDebugEnabled())
        {
            Message messages[] = event.getMessages();
            for (int i = 0; i < messages.length; i++)
            {
                try
                {
                    logger.debug("Message removed: " + messages[i].getSubject());
                }
                catch (MessagingException ignore)
                {
                    logger.debug("ignoring exception: " + ignore.getMessage());
                }
            }
        }
    }

    /**
     * @return the current Mail folder
     */
    public Folder getFolder()
    {
        return folder;
    }

    /**
     * @param folder
     */
    public synchronized void setFolder(Folder folder)
    {
        if (folder == null)
        {
            throw new IllegalArgumentException("Mail folder cannot be null");
        }
        this.folder = folder;
        synchronized (this.folder)
        {
            if (!this.folder.isOpen())
            {
                try
                {
                    this.folder.open(Folder.READ_WRITE);
                }
                catch (MessagingException e)
                {
                    logger.warn("Failed to open folder: " + folder.getFullName(), e);
                }
            }
        }
    }

    /**
     * Helper method for testing which stores a copy of the message locally as the
     * POP3 <p/> message will be deleted from the server
     * 
     * @param msg the message to store
     * @throws IOException If a failure happens writing the message
     * @throws MessagingException If a failure happens reading the message
     */
    protected void storeMessage(Message msg) throws IOException, MessagingException
    {
        if (backupFolder != null)
        {
            String filename = msg.getFileName();
            if (filename == null)
            {
                Address[] from = msg.getFrom();
                if (from != null && from.length > 0)
                {
                    filename = from[0] instanceof InternetAddress
                    ? ((InternetAddress) from[0]).getAddress()
                    : from[0].toString();
                }
                else
                {
                    filename = "(no from address)";
                }
                filename += "[" + UUID.getUUID() + "]";
            }
            filename = FileUtils.prepareWinFilename(filename);
            filename = backupFolder + filename + ".msg";
            if (logger.isDebugEnabled())
            {
                logger.debug("Writing message to: " + filename);
            }
            File f = FileUtils.createFile(filename);
            FileOutputStream fos = new FileOutputStream(f);
            msg.writeTo(fos);
        }
    }

    public synchronized void poll()
    {
        try
        {
            try
            {
                if (!folder.isOpen())
                {
                    folder.open(Folder.READ_WRITE);
                }
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("ignoring exception: " + e.getMessage());
                }
            }

            int count = folder.getMessageCount();
            if (count > 0)
            {
                Message[] messages = folder.getMessages();
                MessageCountEvent event = new MessageCountEvent(folder, MessageCountEvent.ADDED, true,
                    messages);
                messagesAdded(event);
            }
            else if (count == -1)
            {
                throw new MessagingException("Cannot monitor folder: " + folder.getFullName()
                    + " as folder is closed");
            }
        }
        catch (MessagingException e)
        {
            handleException(e);
        }
        finally
        {
            try
            {
                folder.close(true); // close and expunge deleted messages
            }
            catch (Exception e)
            {
                logger.error("Failed to close pop3  inbox: " + e.getMessage());
            }
        }
    }

    protected void doDispose()
    {
        if (null != folder)
        {
            folder.removeMessageCountListener(this);
            if (folder.isOpen())
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

}
