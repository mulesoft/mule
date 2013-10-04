/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ReceiveException;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.email.i18n.EmailMessages;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Poll a mailbox for messages, remove the messages and route them as events into
 * Mule.
 * <p/>
 * This contains a reference to a mail folder (and also the endpoint and connector,
 * via superclasses)
 */
public class RetrieveMessageReceiver extends AbstractPollingMessageReceiver implements MessageCountListener
{
    private Folder folder = null;
    private Folder moveToFolder = null;
    private boolean backupEnabled;
    private String backupFolder = null;
    // A lock to protect concurrent access to the folder.
    private final Object folderLock = new Object();

    public RetrieveMessageReceiver(Connector connector,
                                   FlowConstruct flowConstruct,
                                   InboundEndpoint endpoint,
                                   long checkFrequency,
                                   boolean backupEnabled,
                                   String backupFolder) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.backupFolder = backupFolder;
        this.backupEnabled = backupEnabled;
        this.setFrequency(checkFrequency);
    }

    private AbstractRetrieveMailConnector castConnector()
    {
        return (AbstractRetrieveMailConnector) getConnector();
    }

    @Override
    protected void doConnect() throws Exception
    {
        SessionDetails session = castConnector().getSessionDetails(endpoint);

        Store store = session.newStore();
        store.connect();
        folder = store.getFolder(castConnector().getMailboxFolder());
        if (castConnector().getMoveToFolder() != null)
        {
            moveToFolder = store.getFolder(castConnector().getMoveToFolder());
            moveToFolder.open(Folder.READ_WRITE);
        }

        // set default value if empty/null
        if (StringUtils.isEmpty(backupFolder))
        {
            this.backupFolder = connector.getMuleContext().getConfiguration().getWorkingDirectory()
                                + "/mail/" + folder.getName();
        }

        if (backupFolder != null && !this.backupFolder.endsWith(File.separator))
        {
            this.backupFolder += File.separator;
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // nothing to do here
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        synchronized (folderLock)
        {
            if (folder != null)
            {
                folder.removeMessageCountListener(this);
            }
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();
        synchronized (folderLock)
        {
            folder.addMessageCountListener(this);
        }
    }

    public void messagesAdded(MessageCountEvent event) 
    {
        try
        {
            Message messages[] = event.getMessages();
            List<Message> processedMessages = new ArrayList<Message>();
            if (messages != null)
            {
                MuleMessage message = null;
                for (int i = 0; i < messages.length; i++)
                {
                    if (getLifecycleState().isStopping() || getLifecycleState().isStopped())
                    {
                        break;
                    }
                    try
                    {
                        if (!messages[i].getFlags().contains(Flags.Flag.DELETED)
                            && !messages[i].getFlags().contains(Flags.Flag.SEEN))
                        {
                            try
                            {
                                MimeMessage mimeMessage = new MimeMessage((MimeMessage) messages[i]);
                                storeMessage(mimeMessage);
                                message = createMuleMessage(mimeMessage, endpoint.getEncoding());

                                if (castConnector().isDeleteReadMessages())
                                {
                                    if (moveToFolder != null)
                                    {
                                        folder.copyMessages(new Message[]{messages[i]}, moveToFolder);
                                    }
                                    // Mark as deleted
                                    messages[i].setFlag(Flags.Flag.DELETED, true);
                                }
                                else
                                {
                                    if (this.getEndpoint().getFilter() != null && this.getEndpoint().getFilter().accept(message))
                                    {
                                        Flags.Flag flag = castConnector().getDefaultProcessMessageAction();
                                        if (flag != null)
                                        {
                                            if(flag == Flags.Flag.DELETED && moveToFolder != null)
                                            {
                                                folder.copyMessages(new Message[]{messages[i]}, moveToFolder);
                                            }
                                            messages[i].setFlag(flag, true);
                                        }
                                    }
                                    else
                                    {
                                        messages[i].setFlag(Flags.Flag.SEEN, true);
                                        processedMessages.add(messages[i]);
                                    }
                                }
                                routeMessage(message);
                            }
                            catch (org.mule.api.MessagingException e)
                            {
                                //Already handled by TransactionTemplate
                            }
                            catch (Exception e)
                            {
                                connector.getMuleContext().getExceptionListener().handleException(e);
                                throw e;
                            }
                        }
                    }
                    catch (MuleException e)
                    {
                        throw e;
                    }
                    catch (Exception e)
                    {
                        Exception forwarded;
    
                        if (message != null)
                        {
                            forwarded = new org.mule.api.MessagingException(EmailMessages.routingError(), message, e);
                        }
                        else
                        {
                            forwarded = new ReceiveException(endpoint, -1, e);
                        }
                        throw forwarded;
                    }
                }
                // Copy processed messages that have not been deleted (the deleted were already moved)
                if (moveToFolder != null)
                {
                    folder.copyMessages(processedMessages.toArray(new Message[processedMessages.size()]), moveToFolder);
                }
            }
        }
        catch (Exception e)
        {
            // Throw a runtime exception because the javax.mail API does not allow a checked exception on this method.
            throw new MuleRuntimeException(e);
        }
    }

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

    /** @return the current Mail folder */
    public Folder getFolder()
    {
        return folder;
    }

    /** @param folder */
    public void setFolder(Folder folder)
    {
        synchronized (folderLock)
        {
            if (folder == null)
            {
                throw new IllegalArgumentException("Mail folder cannot be null");
            }
            this.folder = folder;
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
     * POP3
     * <p/>
     * message will be deleted from the server
     *
     * @param msg the message to store
     * @throws IOException If a failure happens writing the message
     * @throws MessagingException If a failure happens reading the message
     */
    protected void storeMessage(Message msg) throws IOException, MessagingException
    {
        if (backupEnabled)
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

    @Override
    public void poll()
    {
        boolean done = false;
        while (!done)
        {
            synchronized (folderLock)
            {
                if (getLifecycleState().isStopping() || getLifecycleState().isStopped())
                {
                    break;
                }
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
                    int batchSize = getBatchSize(count);
                    if (count > 0)
                    {
                        Message[] messages = folder.getMessages(1, batchSize);
                        MessageCountEvent event = new MessageCountEvent(folder, MessageCountEvent.ADDED, true,
                            messages);
                        messagesAdded(event);
                    }
                    else if (count == -1)
                    {
                        throw new MessagingException("Cannot monitor folder: " + folder.getFullName()
                            + " as folder is closed");
                    }
                    done = batchSize >= count;
                }
                catch (MessagingException e)
                {
                    done = true;
                    getConnector().getMuleContext().getExceptionListener().handleException(e);
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
        }
    }


    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }

    @Override
    protected void doDispose()
    {
        synchronized (folderLock)
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

    @Override
    protected MuleMessage handleUnacceptedFilter(MuleMessage message)
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
                logger.error("failed to set message deleted: " + e.getMessage(), e);
            }
        }
        return message;
    }
}
