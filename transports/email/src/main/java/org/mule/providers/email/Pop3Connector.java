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

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * <code>Pop3Connector</code> is used to connect and receive mail from a POP3
 * mailbox.
 */
public class Pop3Connector extends AbstractMailConnector
{
    public static final String MAILBOX = "INBOX";
    public static final int DEFAULT_POP3_PORT = 110;
    public static final int DEFAULT_CHECK_FREQUENCY = 60000;

    /**
     * Holds the time in milliseconds that the endpoint should wait before checking a
     * mailbox
     */
    protected volatile long checkFrequency = DEFAULT_CHECK_FREQUENCY;

    /**
     * holds a path where messages should be backed up to
     */
    protected volatile String backupFolder = null;

    /**
     * Once a message has been read, should it be deleted
     */
    protected volatile boolean deleteReadMessages = true;

    public Pop3Connector()
    {
        super();
    }

    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // template method, nothing to do
    }

    protected void doConnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doDisconnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doStart() throws UMOException
    {
        // template method, nothing to do
    }

    protected void doStop() throws UMOException
    {
        // template method, nothing to do
    }

    /**
     * @return the milliseconds between checking the folder for messages
     */
    public long getCheckFrequency()
    {
        return checkFrequency;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "pop3";
    }

    /**
     * @param l
     */
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

    /**
     * @param string
     */
    public void setBackupFolder(String string)
    {
        backupFolder = string;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#registerListener(javax.jms.MessageListener,
     *      java.lang.String)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        Object[] args = {new Long(checkFrequency), backupFolder};
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, args);
    }

    public int getDefaultPort()
    {
        return DEFAULT_POP3_PORT;
    }

    public boolean isDeleteReadMessages()
    {
        return deleteReadMessages;
    }

    public void setDeleteReadMessages(boolean deleteReadMessages)
    {
        this.deleteReadMessages = deleteReadMessages;
    }

}
