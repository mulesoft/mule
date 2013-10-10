/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;

import javax.mail.Flags;

/**
 * Support for connecting to and receiving email from a mailbox (the exact protocol depends on
 * the subclass).
 */
public abstract class AbstractRetrieveMailConnector extends AbstractMailConnector
{
    public static final int DEFAULT_CHECK_FREQUENCY = 60000;

    /**
     * Holds the time in milliseconds that the endpoint should wait before checking a
     * mailbox
     */
    private volatile long checkFrequency = DEFAULT_CHECK_FREQUENCY;

    /**
     * Holds a path where messages should be backed up to (auto-generated if empty)
     */
    private volatile String backupFolder = null;

    /**
     * Holds a remote folder name where messages should be moved to after being read
     */
    private volatile String moveToFolder = null;

    /**
     * Should we save backups to backupFolder?
     */
    private boolean backupEnabled = false;

    /**
     * Once a message has been read, should it be deleted
     */
    private volatile boolean deleteReadMessages = true;

    /**
     * The action performed if the deleteReadMessages actions is set to false
     */
    private Flags.Flag defaultProcessMessageAction = Flags.Flag.SEEN;


    protected AbstractRetrieveMailConnector(int defaultPort, MuleContext context)
    {
        super(defaultPort, MAILBOX, context);
    }

    /**
     * @return the milliseconds between checking the folder for messages
     */
    public long getCheckFrequency()
    {
        return checkFrequency;
    }

    public void setCheckFrequency(long l)
    {
        if (l < 1)
        {
            l = DEFAULT_CHECK_FREQUENCY;
        }
        checkFrequency = l;
    }

    /**
     * @return a relative or absolute path to a directory on the file system
     */
    public String getBackupFolder()
    {
        return backupFolder;
    }

    public void setBackupFolder(String string)
    {
        backupFolder = string;
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        Object[] args = {checkFrequency, isBackupEnabled(), backupFolder};
        return serviceDescriptor.createMessageReceiver(this, flowConstruct, endpoint, args);
    }

    public boolean isDeleteReadMessages()
    {
        return deleteReadMessages;
    }

    public void setDeleteReadMessages(boolean deleteReadMessages)
    {
        this.deleteReadMessages = deleteReadMessages;
    }

    public boolean isBackupEnabled()
    {
        return backupEnabled;
    }

    public void setBackupEnabled(boolean backupEnabled)
    {
        this.backupEnabled = backupEnabled;
    }

    public String getMoveToFolder()
    {
        return moveToFolder;
    }

    public void setMoveToFolder(String moveToFolder)
    {
        this.moveToFolder = moveToFolder;
    }

    public Flags.Flag getDefaultProcessMessageAction()
    {
        return defaultProcessMessageAction;
    }

    public void setDefaultProcessMessageAction(Flags.Flag defaultProcessMessageAction)
    {
        this.defaultProcessMessageAction = defaultProcessMessageAction;
    }
}
