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

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;

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
     * Should we save backups to backupFolder?
     */
    private boolean backupEnabled = false;

    /**
     * Once a message has been read, should it be deleted
     */
    private volatile boolean deleteReadMessages = true;

    
    protected AbstractRetrieveMailConnector(int defaultPort)
    {
        super(defaultPort, MAILBOX);
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

    /**
     * @see org.mule.api.transport.Connector#registerListener(org.mule.api.service.Service, org.mule.api.endpoint.InboundEndpoint) 
     */
    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        Object[] args = {checkFrequency, isBackupEnabled(), backupFolder};
        return serviceDescriptor.createMessageReceiver(this, service, endpoint, args);
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

}
