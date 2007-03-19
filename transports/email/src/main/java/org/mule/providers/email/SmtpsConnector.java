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

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.TlsSupport;

import java.io.IOException;

/**
 * Creates a secure SMTP connection
 */
public class SmtpsConnector extends SmtpConnector
{

    public static final String DEFAULT_SOCKET_FACTORY = "javax.net.ssl.SSLSocketFactory";

    private String socketFactory = DEFAULT_SOCKET_FACTORY;
    private String socketFactoryFallback = "false";
    private TlsSupport tlsSupport = new TlsSupport();

    public static final int DEFAULT_SMTPS_PORT = 465;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "smtps";
    }

    public int getDefaultPort()
    {
        return DEFAULT_SMTPS_PORT;
    }

    protected void doInitialise() throws InitialisationException
    {
        tlsSupport.initialiseFactories(true);
        System.setProperty("mail.smtps.ssl", "true");
        System.setProperty("mail.smtps.socketFactory.class", getSocketFactory());
        System.setProperty("mail.smtps.socketFactory.fallback", getSocketFactoryFallback());
        tlsSupport.initialiseStores();
    }

    public String getSocketFactory()
    {
        return socketFactory;
    }

    public void setSocketFactory(String sslSocketFactory)
    {
        this.socketFactory = sslSocketFactory;
    }

    public String getSocketFactoryFallback()
    {
        return socketFactoryFallback;
    }

    public void setSocketFactoryFallback(String socketFactoryFallback)
    {
        this.socketFactoryFallback = socketFactoryFallback;
    }

    public String getTrustStore()
    {
        return tlsSupport.getTrustStore();
    }

    public String getTrustStorePassword()
    {
        return tlsSupport.getTrustStorePassword();
    }

    public void setTrustStore(String trustStore) throws IOException
    {
        tlsSupport.setTrustStore(trustStore);
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tlsSupport.setTrustStorePassword(trustStorePassword);
    }


}
