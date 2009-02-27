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

import java.util.Properties;

/**
 * <code>SmtpConnector</code> is used to connect to and send data to an SMTP mail
 * server
 */
public class SmtpConnector extends AbstractMailConnector
{

    public static final String SMTP = "smtp";
    public static final String DEFAULT_SMTP_HOST = "localhost";
    public static final int DEFAULT_SMTP_PORT = 25;
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

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

    
    public SmtpConnector()
    {
        this(DEFAULT_SMTP_PORT);
    }
    
    SmtpConnector(int defaultPort)
    {
        super(defaultPort, null);
    }
    
    public String getProtocol()
    {
        return "smtp";
    }

    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        throw new UnsupportedOperationException("Listeners cannot be registered on a SMTP endpoint");
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

}
