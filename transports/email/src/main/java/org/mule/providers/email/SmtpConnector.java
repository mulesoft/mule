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
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.mail.Authenticator;
import java.util.Properties;

/**
 * <code>SmtpConnector</code> is used to connect to and send data to an SMTP
 * mail server
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SmtpConnector extends AbstractServiceEnabledConnector implements MailConnector
{
    public static final String DEFAULT_SMTP_HOST = "localhost";
    public static final int DEFAULT_SMTP_PORT = 25;
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

    private String host = DEFAULT_SMTP_HOST;
    private int port = DEFAULT_SMTP_PORT;
    private String username;
    private String password;

    /**
     * Holds value of bcc addresses.
     */
    private String bcc;

    /**
     * Holds value of cc addresses.
     */
    private String cc;

    /**
     * Holds value of replyTo addresses.
     */
    private String replyTo;

    /**
     * Holds value of default subject
     */
    private String defaultSubject = "[No Subject]";

    /**
     * Holds value of the from address.
     */
    private String from;

    /**
     * Any custom headers to be set on messages sent using this connector
     */
    private Properties customHeaders = new Properties();

    /**
     * A custom authenticator to be used on any mail sessions created with this connector
     * This will only be used if user name credentials are set on the endpoint
     */
    private Authenticator authenticator = null;

    private String contentType = DEFAULT_CONTENT_TYPE;

    public SmtpConnector() throws InitialisationException
    {
        initFromServiceDescriptor();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.UMOConnector#registerListener(javax.jms.MessageListener,
     *      java.lang.String)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        throw new UnsupportedOperationException("Listeners cannot be registered on a SMTP endpoint");
    }

    /*
     * @see org.mule.providers.UMOConnector#start()
     */
    public void doStart() throws UMOException
    {
        // template method
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.UMOConnector#stop()
     */
    public void doStop() throws UMOException
    {
        // template method
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.AbstractConnector#doDispose()
     */
    protected void doDispose()
    {
        try {
            doStop();
        } catch (UMOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public String getProtocol()
    {
        return "smtp";
    }

    /////////////////////////////////////////////////////////////////////////
    // Getters and setters
    /////////////////////////////////////////////////////////////////////////

     /**
      * @return The default from address to use
      */
     public String getFromAddress()
     {
         return from;
     }

     /**
     * @return the default comma separated list of BCC addresses to use
     */
    public String getBccAddresses()
    {
        return bcc;
    }

    /**
     * @return the default comma separated list of CC addresses to use
     */
    public String getCcAddresses()
    {
        return cc;
    }

    /**
     * @return the default message subject to use
     */
    public String getSubject()
    {
        return defaultSubject;
    }


    public void setBccAddresses(String string)
    {
        bcc = string;
    }

    public void setCcAddresses(String string)
    {
        cc = string;
    }

    public void setSubject(String string)
    {
        defaultSubject = string;
    }

    public void setFromAddress(String string)
    {
        from = string;
    }

    public String getReplyToAddresses() {
        return replyTo;
    }

    public void setReplyToAddresses(String replyTo) {
        this.replyTo = replyTo;
    }

    public Properties getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(Properties customHeaders) {
        this.customHeaders = customHeaders;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getDefaultPort() {
        return DEFAULT_SMTP_PORT;
    }


    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public int getPort() {
        return port;
    }


    public void setPort(int port) {
        this.port = port;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }
}
