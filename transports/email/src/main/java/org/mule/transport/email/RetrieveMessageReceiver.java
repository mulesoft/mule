/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.execution.ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
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
import java.nio.channels.NotYetConnectedException;
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

    private static final String FOLDER_EXCEPTION_FORMAT = "Unexpected exception %s folder %s : %s";
    private Folder folder = null;
    private Folder moveToFolder = null;
    private boolean backupEnabled;
    private String backupFolder = null;
    // A lock to protect concurrent access to the folder.
    private final Object folderLock = new Object();
    private final Object connectionLock = new Object();


    private boolean connectionEstablished = false;

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
        // Execute listener connection with the connector's retry policy
        getConnector().getRetryPolicyTemplate().execute(new RetryCallback()
        {
            @Override
            public void doWork(RetryContext context) throws Exception
            {
                synchronized (connectionLock)
                {
                    logger.info("Attemping to establish connection with message retrieval server");
                    SessionDetails session = RetrieveMessageReceiver.this.castConnector().getSessionDetails(endpoint);

                    Store store = session.newStore();
                    store.connect();
                    RetrieveMessageReceiver.this.folder = store.getFolder(castConnector().getMailboxFolder());
                    if (castConnector().getMoveToFolder() != null)
                    {
                        RetrieveMessageReceiver.this.moveToFolder = store.getFolder(castConnector().getMoveToFolder());
                        RetrieveMessageReceiver.this.moveToFolder.open(READ_WRITE);
                    }

                    // set default value if empty/null
                    if (StringUtils.isEmpty(backupFolder))
                    {
                        RetrieveMessageReceiver.this.backupFolder = getEndpoint().getMuleContext().getConfiguration().getWorkingDirectory()
                                + "/mail/" + folder.getName();
                    }

                    if (RetrieveMessageReceiver.this.backupFolder != null && !RetrieveMessageReceiver.this.backupFolder.endsWith(File.separator))
                    {
                        RetrieveMessageReceiver.this.backupFolder += File.separator;
                    }

                    RetrieveMessageReceiver.this.connectionEstablished = true;
                }
            }

            @Override
            public String getWorkDescription()
            {
                return "Trying to connect to email server: " + RetrieveMessageReceiver.this.getConnectionDescription();
            }

            @Override
            public Object getWorkOwner()
            {
                return RetrieveMessageReceiver.this;
            }
        }, getWorkManager());
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
        try
        {
            getConnector().getRetryPolicyTemplate().execute(new RetryCallback()
            {
                @Override
                public void doWork(RetryContext retryContext) throws MuleException
                {
                    synchronized (connectionLock)
                    {
                        if (RetrieveMessageReceiver.this.connectionEstablished)
                        {
                            RetrieveMessageReceiver.super.doStart();
                            synchronized (folderLock)
                            {
                                folder.addMessageCountListener(RetrieveMessageReceiver.this);
                            }
                        }
                        else
                        {
                            throw new NotYetConnectedException();
                        }
                    }
                }

                @Override
                public String getWorkDescription() {
                    return "Trying to start the retrieve message retriever";
                }

                @Override
                public Object getWorkOwner() {
                    return RetrieveMessageReceiver.this;
                }
            }, getWorkManager());
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            // Ignore other exceptions
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
                        if (shouldProcessMessage(messages[i]))
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
                                    messages[i].setFlag(DELETED, true);
                                }
                                else
                                {
                                    if (this.getEndpoint().getFilter() != null && this.getEndpoint().getFilter().accept(message))
                                    {
                                        Flags.Flag flag = castConnector().getDefaultProcessMessageAction();
                                        if (flag != null)
                                        {
                                            if(flag == DELETED && moveToFolder != null)
                                            {
                                                folder.copyMessages(new Message[]{messages[i]}, moveToFolder);
                                            }
                                            messages[i].setFlag(flag, true);
                                        }
                                    }
                                    else
                                    {
                                        messages[i].setFlag(SEEN, true);
                                        processedMessages.add(messages[i]);
                                    }
                                }

                                final MuleMessage routedMessage = message;
                                createErrorHandlingExecutionTemplate(getEndpoint().getMuleContext(), null)
                                        .execute(new ExecutionCallback<MuleEvent>()
                                        {
                                            @Override
                                            public MuleEvent process() throws Exception
                                            {
                                                return routeMessage(routedMessage);
                                            }
                                        });
                            }
                            catch (org.mule.api.MessagingException e)
                            {
                                //Already handled by ErrorHandlingExecutionTemplate
                            }
                            catch (Exception e)
                            {
                                getEndpoint().getMuleContext().getExceptionListener().handleException(e);
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
                catch (MessagingException e)
                {
                    logger.debug(String.format("Unexpected exception getting subject: %s", e.getMessage()));
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
        int offset = 1;
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
                        handleFolderException("opening", e);
                    }

                    //total messages in folder
                    int count = folder.getMessageCount();
                    //amount that can be processed
                    int batchSize = getBatchSize(count);
                    if (count > 0)
                    {
                        //retrieve batchSize messages at most, considering the offset that might be present
                        int limit = Math.min(count, offset + batchSize - 1);
                        Message[] messages = folder.getMessages(offset, limit);
                        boolean newMessagesReceived = containsNewMessages(messages);
                        MessageCountEvent event = new MessageCountEvent(folder, MessageCountEvent.ADDED, true,
                            messages);
                        messagesAdded(event);

                        // If the processed messages are not deleted, or if current batch doesn't have new mails (already
                        // marked as read in the server), move the offset forward to not consider them next.
                        if (!castConnector().isDeleteReadMessages() || !newMessagesReceived)
                        {
                            offset += batchSize;
                        }
                    }
                    else if (count == -1)
                    {
                        throw new MessagingException("Cannot monitor folder: " + folder.getFullName()
                            + " as folder is closed");
                    }
                    //stop if the total or current processed messages exceed the total amount
                    done = (offset >= count) || (batchSize >= count);
                }
                catch (MessagingException e)
                {
                    done = true;
                    getEndpoint().getMuleContext().getExceptionListener().handleException(e);
                }
                finally
                {
                    try
                    {
                        closeFolder();
                    }
                    catch (Exception e)
                    {
                        logger.error(String.format(FOLDER_EXCEPTION_FORMAT, "closing", folder.getFullName(), e.getMessage()));
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
                try
                {
                    closeFolder();
                }
                catch (Exception e)
                {
                    handleFolderException("closing", e);
                }
            }
        }
    }

    private void closeFolder() throws MessagingException
    {
        if (folder != null && folder.isOpen())
        {
            folder.close(true); // close and expunge deleted messages
        }
    }

    private void handleFolderException(String operation, Exception e)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(String.format(FOLDER_EXCEPTION_FORMAT, operation, folder.getFullName(), e.getMessage()), e);
        }
    }

    private boolean containsNewMessages(Message[] messages) throws MessagingException
    {
        for (Message message : messages)
        {
            if (shouldProcessMessage(message))
            {
                return true;
            }
        }
        return false;
    }

    private boolean shouldProcessMessage(Message message) throws MessagingException
    {
        return !message.getFlags().contains(DELETED) && !message.getFlags().contains(SEEN);
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
                msg.setFlag(DELETED, endpoint.isDeleteUnacceptedMessages());
            }
            catch (MessagingException e)
            {
                logger.error("failed to set message deleted: " + e.getMessage(), e);
            }
        }
        return message;
    }
}
