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
 * Creates a secure connection to a POP3 mailbox
 */
public class Pop3sConnector extends Pop3Connector
{
    public static final String DEFAULT_SOCKET_FACTORY = "javax.net.ssl.SSLSocketFactory";
    public static final int DEFAULT_POP3S_PORT = 995;

    private String socketFactory = DEFAULT_SOCKET_FACTORY;
    private String socketFactoryFallback = "false";
    private TlsSupport tlsSupport = new TlsSupport();

    public String getProtocol()
    {
        return "pop3s";
    }

    public int getDefaultPort()
    {
        return DEFAULT_POP3S_PORT;
    }

    protected void doInitialise() throws InitialisationException
    {
        tlsSupport.initialiseFactories(true);
        super.doInitialise();
        System.setProperty("mail." + getProtocol() + ".ssl", "true");
        System.setProperty("mail." + getProtocol() + ".socketFactory.class", getSocketFactory());
        System.setProperty("mail." + getProtocol() + ".socketFactory.fallback", getSocketFactoryFallback());
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
