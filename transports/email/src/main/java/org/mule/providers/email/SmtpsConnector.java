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

import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.TlsIndirectKeyStore;
import org.mule.umo.security.TlsIndirectTrustStore;
import org.mule.umo.security.tls.TlsConfiguration;
import org.mule.umo.security.tls.TlsPropertiesMapper;

import java.io.IOException;
import java.util.Properties;

import javax.mail.URLName;

/** Creates a secure SMTP connection */
public class SmtpsConnector extends SmtpConnector implements TlsIndirectTrustStore, TlsIndirectKeyStore
{

    public static final String DEFAULT_SOCKET_FACTORY = SmtpsSocketFactory.class.getName();

    private String socketFactory = DEFAULT_SOCKET_FACTORY;
    private String socketFactoryFallback = "false";
    private TlsConfiguration tls = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);

    public static final int DEFAULT_SMTPS_PORT = 465;


    public SmtpsConnector()
    {
        super(DEFAULT_SMTPS_PORT);
    }

    public String getProtocol()
    {
        return "smtps";
    }

    public String getBaseProtocol()
    {
        return "smtp";
    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            tls.initialise(true, null);
        }
        catch (CreateException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    // @Override
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url)
    {
        super.extendPropertiesForSession(global, local, url);

        local.setProperty("mail." + getProtocol() + ".ssl", "true");
        local.setProperty("mail." + getProtocol() + ".socketFactory.class", getSocketFactory());
        local.setProperty("mail." + getProtocol() + ".socketFactory.fallback", getSocketFactoryFallback());

        new TlsPropertiesMapper(SmtpsSocketFactory.MULE_SMTPS_NAMESPACE).writeToProperties(global, tls);
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
        return tls.getTrustStore();
    }

    public String getTrustStorePassword()
    {
        return tls.getTrustStorePassword();
    }

    public void setTrustStore(String trustStore) throws IOException
    {
        tls.setTrustStore(trustStore);
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tls.setTrustStorePassword(trustStorePassword);
    }

    // these were not present before, but could be set implicitly via global proeprties
    // that is no longer true, so i have added them in here

    public String getClientKeyStore()
    {
        return this.tls.getClientKeyStore();
    }

    public String getClientKeyStorePassword()
    {
        return this.tls.getClientKeyStorePassword();
    }

    public String getClientKeyStoreType()
    {
        return this.tls.getClientKeyStoreType();
    }

    public void setClientKeyStore(String name) throws IOException
    {
        this.tls.setClientKeyStore(name);
    }

    public void setClientKeyStorePassword(String clientKeyStorePassword)
    {
        this.tls.setClientKeyStorePassword(clientKeyStorePassword);
    }

    public void setClientKeyStoreType(String clientKeyStoreType)
    {
        this.tls.setClientKeyStoreType(clientKeyStoreType);
    }

}
