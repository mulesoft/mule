/*
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.mail.*;
import javax.mail.event.MessageCountListener;
import java.util.Properties;


/**
 * <code>Pop3Connector</code> is used to connect and receive mail from a
 * pop3  mailbox
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Pop3Connector extends AbstractServiceEnabledConnector
{
    public static final String MAILBOX = "INBOX";
    public static final int DEFAULT_POP3_PORT = 110;
    public static final int DEFAULT_CHECK_FREQUENCY = 60000;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(Pop3Connector.class);

    /**
     * Holds the time in milliseconds that the endpoint should wait before checking a mailbox
     */
    private long checkFrequency = DEFAULT_CHECK_FREQUENCY;

    /**
     * Holds value of property password.
     */
    private String password;

    /**
     * Holds value of property username.
     */
    private String username;

    /**
     * Holds value of property host.
     */
    private String hostname = "localhost";

    /**
     * Holds value of property server port.
     */
    private int port = DEFAULT_POP3_PORT;

    /**
     * holds a path where messages should be backed up to
     */
    private String backupFolder = null;

    private Session session;

    private Folder inbox = null;

    public Pop3Connector() throws Exception
    {
        //for testing purposes I've had to make this call in the constructor
        //so that the connector can be tested without connectoing to a pop3 mailbox
        super.doInitialise();
    }

    public void doInitialise() throws InitialisationException
    {
        URLName url = new URLName(getProtocol(), getHostname(),
                getPort(), getMailBox(), getUsername(), getPassword());

        Properties props = System.getProperties();
        props.put("mail.smtp.host", getHostname());
        props.put("mail.smtp.port", String.valueOf(getPort()));
        session = Session.getDefaultInstance(props, null);
        session.setDebug(logger.isDebugEnabled());
        PasswordAuthentication pw = new PasswordAuthentication(getUsername(), getPassword());
        session.setPasswordAuthentication(url, pw);

        try
        {
            Store store = session.getStore(url);
            store.connect();
            inbox = store.getFolder(getMailBox());
        } catch (MessagingException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * @return
     */
    public long getCheckFrequency()
    {
        return checkFrequency;
    }

    /**
     * Getter for property password.
     *
     * @return Value of property password.
     */
    public String getPassword()
    {
        return this.password;
    }

    Folder getInbox() {
        return inbox;
    }

    Session getSession()
    {
        return session;
    }

    /**
     * Getter for property hostname of the pop3 server.
     *
     * @return Value of property hostname.
     */
    public String getHostname()
    {
        return hostname;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "pop3";
    }

    /**
     * Getter for property username.
     *
     * @return Value of property username.
     */
    public String getUsername()
    {
        return this.username;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#registerListener(javax.jms.MessageListener, java.lang.String)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        UMOMessageReceiver receiver = serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{inbox, new Long(checkFrequency), backupFolder});
        inbox.addMessageCountListener((MessageCountListener)receiver);
        return receiver;
    }

    /**
     * @param l
     */
    public void setCheckFrequency(long l)
    {
        if(l < 1) l = DEFAULT_CHECK_FREQUENCY;
        checkFrequency = l;
    }


    /**
     * Setter for property password.
     *
     * @param password New value of property password.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }


    /**
     * Setter for property hostname of the pop3 server.
     *
     * @param hostname New value of property hostname.
     */
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    /**
     * Setter for property username.
     *
     * @param username New value of property username.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#start()
     */
    public void startConnector() throws UMOException
    {
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#stop()
     */
    public void stopConnector() throws UMOException
    {
        try
        {
            if(inbox!=null) inbox.getStore().close();
        }
        catch (MessagingException e)
        {
            logger.error("Failed to close Email store: " + e, e);
        }
    }



    /* (non-Javadoc)

     * @see org.mule.providers.AbstractConnector#disposeConnector()

     */

    protected void disposeConnector()
    {
        try {
            stopConnector();
        } catch (UMOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * @return
     */
    public String getBackupFolder()
    {
        return backupFolder;
    }


    public String getMailBox()
    {
        return MAILBOX;
    }

    /**
     * @param string
     */
    public void setBackupFolder(String string)
    {
        backupFolder = string;
    }

}