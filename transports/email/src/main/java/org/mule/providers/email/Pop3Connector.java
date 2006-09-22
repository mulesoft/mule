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

import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.mail.Authenticator;

/**
 * <code>Pop3Connector</code> is used to connect and receive mail from a pop3
 * mailbox
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Pop3Connector extends AbstractServiceEnabledConnector implements MailConnector
{
    public static final String MAILBOX = "INBOX";
    public static final int DEFAULT_POP3_PORT = 110;
    public static final int DEFAULT_CHECK_FREQUENCY = 60000;

    /**
     * Holds the time in milliseconds that the endpoint should wait before
     * checking a mailbox
     */
    protected long checkFrequency = DEFAULT_CHECK_FREQUENCY;

    /**
     * holds a path where messages should be backed up to
     */
    protected String backupFolder = null;

     /**
     * A custom authenticator to bew used on any mail sessions created with this connector
     * This will only be used if user name credendtials are set on the endpoint
     */
    protected Authenticator authenticator = null;

    /**
     * Once a message has been read, should it be deleted
     */
    protected boolean deleteReadMessages = true;

    public Pop3Connector() {
        super();
        //by default, close client connections to pop3 after the request.
        this.setCreateDispatcherPerRequest(true);
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
        if (l < 1) {
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

    /* (non-Javadoc) 
     * @see org.mule.providers.UMOConnector#registerListener(javax.jms.MessageListener, java.lang.String) 
     */ 
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception 
    {
        Object[] args = {new Long(checkFrequency), backupFolder};
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, args); 
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public int getDefaultPort() {
        return DEFAULT_POP3_PORT;
    }

    public boolean isDeleteReadMessages() {
        return deleteReadMessages;
    }

    public void setDeleteReadMessages(boolean deleteReadMessages) {
        this.deleteReadMessages = deleteReadMessages;
    }
}
