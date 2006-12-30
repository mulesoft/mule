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

import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.URLName;

import org.apache.commons.lang.StringUtils;

/**
 * <code>Pop3Connector</code> is used to connect and receive mail from a POP3
 * mailbox
 */
public class Pop3Connector extends AbstractServiceEnabledConnector implements MailConnector
{
    public static final String MAILBOX = "INBOX";
    public static final int DEFAULT_POP3_PORT = 110;
    public static final int DEFAULT_CHECK_FREQUENCY = 60000;

    /**
     * Holds the time in milliseconds that the endpoint should wait before checking a
     * mailbox
     */
    protected long checkFrequency = DEFAULT_CHECK_FREQUENCY;

    /**
     * holds a path where messages should be backed up to
     */
    protected String backupFolder = null;

    /**
     * A custom authenticator to bew used on any mail sessions created with this
     * connector This will only be used if user name credendtials are set on the
     * endpoint
     */
    protected Authenticator authenticator = null;

    /**
     * Once a message has been read, should it be deleted
     */
    protected boolean deleteReadMessages = true;

    public Pop3Connector()
    {
        super();
        // by default, close client connections to pop3 after the request.
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

    public Authenticator getAuthenticator()
    {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator)
    {
        this.authenticator = authenticator;
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

    /**
     * This implementation of UMOConnector.getDelegateSession() creates a new
     * javax.mail Session based on a URL. If a password is set on the URL it also
     * adds an SMTP authenticator.
     * 
     * @param endpoint the endpoint for which the session is needed
     * @param args a javax.mail.URLName providing additional properties of the
     *            required Session (host, port etc.)
     */
    public Object getDelegateSession(UMOImmutableEndpoint endpoint, Object args)
    {
        URLName url = (URLName)args;

        if (url == null)
        {
            throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.X_IS_NULL, "URL")
                .toString());
        }

        String protocol = this.getProtocol().toLowerCase();
        boolean secure = false;

        if (protocol.equals("smtps"))
        {
            protocol = "smtp";
            secure = true;
        }
        else if (protocol.equals("pop3s"))
        {
            protocol = "pop3";
            secure = true;
        }
        else if (protocol.equals("imaps"))
        {
            protocol = "imap";
            secure = true;
        }

        Properties props = System.getProperties();
        Session session;

        // make sure we do not mess with authentication set via system properties
        synchronized (props)
        {
            props.put("mail." + protocol + ".host", url.getHost());

            int port = url.getPort();
            if (port == -1)
            {
                port = this.getDefaultPort();
            }

            props.put("mail." + protocol + ".port", String.valueOf(port));

            // a somewhat obscure way of enabling typecasting depending on protocol
            MailConnector connector = this;

            if (secure)
            {
                System.setProperty("mail." + protocol + ".socketFactory.port", String.valueOf(port));
                if (protocol.equals("smtp"))
                {
                    // these following properties should not be set on the System
                    // properties as well since they will conflict with the smtp
                    // properties.
                    props = (Properties)props.clone();

                    props.put("mail.smtp.ssl", "true");
                    props
                        .put("mail.smtp.socketFactory.class", ((SmtpsConnector)connector).getSocketFactory());
                    props.put("mail.smtp.socketFactory.fallback", ((SmtpsConnector)connector)
                        .getSocketFactoryFallback());

                    if (((SmtpsConnector)connector).getTrustStore() != null)
                    {
                        System.setProperty("javax.net.ssl.trustStore", ((SmtpsConnector)connector)
                            .getTrustStore());
                        if (((SmtpsConnector)connector).getTrustStorePassword() != null)
                        {
                            System.setProperty("javax.net.ssl.trustStorePassword",
                                ((SmtpsConnector)connector).getTrustStorePassword());
                        }
                    }
                }
            }

            props.setProperty("mail." + protocol + ".rsetbeforequit", "true");

            if (StringUtils.isNotBlank(url.getPassword()))
            {
                props.put("mail." + protocol + ".auth", "true");
                Authenticator auth = connector.getAuthenticator();
                if (auth == null)
                {
                    auth = new DefaultAuthenticator(url.getUsername(), url.getPassword());
                    logger.debug("No Authenticator set on Connector: " + connector.getName()
                                    + ". Using default.");
                }
                session = Session.getInstance(props, auth);
            }
            else
            {
                // reset authentication property so smtp is not affected (MULE-464)
                props.put("mail." + protocol + ".auth", "false");
                session = Session.getInstance(props, null);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Creating mail session: host = " + url.getHost() + ", port = " + url.getPort()
                            + ", user = " + url.getUsername() + ", pass = " + url.getPassword());
        }

        return session;
    }

}
