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
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.StringUtils;

import java.util.Properties;

import javax.mail.URLName;

/**
 * <code>SmtpConnector</code> is used to connect to and send data to an SMTP mail
 * server
 */
public class SmtpConnector extends AbstractMailConnector
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

    private String contentType = DEFAULT_CONTENT_TYPE;

    //todo RM*: This doesn't look right. The init should be done in the super class in initialise
//    public SmtpConnector() throws InitialisationException
//    {
//        initFromServiceDescriptor();
//    }

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


    protected void doInitialise() throws InitialisationException
    {
        //template method, nothing to do
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

    public String getProtocol()
    {
        return "smtp";
    }

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

    public String getReplyToAddresses()
    {
        return replyTo;
    }

    public void setReplyToAddresses(String replyTo)
    {
        this.replyTo = replyTo;
    }

    public Properties getCustomHeaders()
    {
        return customHeaders;
    }

    public void setCustomHeaders(Properties customHeaders)
    {
        this.customHeaders = customHeaders;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public int getDefaultPort()
    {
        return DEFAULT_SMTP_PORT;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * We override the base implementation in AbstractMailConnector to create a
     * proper URLName if none was given. This allows javax.mail.Message creation
     * without access to connection information in the caller (see
     * StringToEmailMessage). This is a workaround for the dependency of
     * StringToEmailMessage on the connection information which only available in
     * this class.
     */
    public Object getDelegateSession(UMOImmutableEndpoint endpoint, Object args)
    {
        URLName url = (URLName)args;

        // build required URLName unless already provided
        if (url == null)
        {
            UMOEndpointURI uri = endpoint.getEndpointURI();

            // Try to get the properties from the endpoint and use the connector
            // properties if they are not given.

            String host = uri.getHost();
            if (host == null)
            {
                host = this.getHost();
            }

            int port = uri.getPort();
            if (port == -1)
            {
                port = this.getPort();
            }

            String username = uri.getUsername();
            if (StringUtils.isBlank(username))
            {
                username = this.getUsername();
            }

            String password = uri.getPassword();
            if (StringUtils.isBlank(password))
            {
                password = this.getPassword();
            }

            url = new URLName(this.getProtocol(), host, port, null, username, password);
        }

        return super.getDelegateSession(endpoint, url);
    }

}
