/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.TlsIndirectKeyStore;
import org.mule.api.security.TlsIndirectTrustStore;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.api.security.tls.TlsPropertiesMapper;

import java.io.IOException;
import java.util.Properties;

import javax.mail.URLName;

/**
 * Support for connecting to and receiving email from a secure mailbox (the exact protocol depends on
 * the subclass).
 */
public abstract class AbstractTlsRetrieveMailConnector
        extends AbstractRetrieveMailConnector implements TlsIndirectTrustStore, TlsIndirectKeyStore
{

    private String namespace;
    private String socketFactory;
    private String socketFactoryFallback = "false";
    private TlsConfiguration tls = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);

    protected AbstractTlsRetrieveMailConnector(int defaultPort, String namespace, Class defaultSocketFactory, MuleContext context)
    {
        super(defaultPort, context);
        this.namespace = namespace;
        socketFactory = defaultSocketFactory.getName();

        // see comment below
//        this.namespace = TlsConfiguration.JSSE_NAMESPACE;
//        socketFactory = SSLServerSocketFactory.class.getName();
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
        super.doInitialise();
    }

    @Override
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url)
    {
        super.extendPropertiesForSession(global, local, url);

        local.setProperty("mail." + getProtocol() + ".ssl", "true");
        local.setProperty("mail." + getProtocol() + ".socketFactory.class", getSocketFactory());
        local.setProperty("mail." + getProtocol() + ".socketFactory.fallback", getSocketFactoryFallback());

        new TlsPropertiesMapper(namespace).writeToProperties(global, tls);

        // this, instead of the line above, and with the constructor changes,
        // would have changed to local SSL configuration, if that was possible
        // (it didn't work)
//        new TlsPropertiesMapper(namespace).writeToProperties(local, tls);
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

    // these were not present before, but could be set implicitly via global properties
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
