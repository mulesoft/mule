/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.TlsDirectKeyStore;
import org.mule.api.security.TlsDirectTrustStore;
import org.mule.api.security.TlsIndirectKeyStore;
import org.mule.api.security.TlsProtocolHandler;
import org.mule.api.security.provider.SecurityProviderFactory;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.util.SystemUtils;

import java.io.IOException;
import java.security.Provider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * The <code>JettyHttpsConnector</code> can be using to embed a Jetty server to receive requests on an http inbound endpoint.
 * One server is created for each connector declared, many Jetty endpoints can share the same connector.
 */

public class JettyHttpsConnector extends JettyHttpConnector implements TlsDirectKeyStore, TlsIndirectKeyStore, TlsDirectTrustStore, TlsProtocolHandler
{

    public static final String JETTY_SSL = "jetty-ssl";
    public static final String HTTPS = "https";
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
    public static final String LOCAL_CERTIFICATES = "LOCAL_CERTIFICATES";

    private TlsConfiguration tls = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);

    public JettyHttpsConnector(MuleContext context)
    {
        super(context);
        registerSupportedProtocol("https");
        registerSupportedProtocol("jetty-ssl");
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        validateSslConfig();
        super.doInitialise();
    }

    protected void validateSslConfig() throws InitialisationException
    {
        try
        {
            tls.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
        }
        catch (CreateException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public String getProtocol()
    {
        return JETTY_SSL;
    }

    public String getClientKeyStore()
    {
        return tls.getClientKeyStore();
    }

    public String getClientKeyStorePassword()
    {
        return tls.getClientKeyStorePassword();
    }

    public String getClientKeyStoreType()
    {
        return this.tls.getClientKeyStoreType();
    }

    public String getKeyManagerAlgorithm()
    {
        return tls.getKeyManagerAlgorithm();
    }

    public KeyManagerFactory getKeyManagerFactory()
    {
        return tls.getKeyManagerFactory();
    }

    public String getKeyPassword()
    {
        return tls.getKeyPassword();
    }

    public String getKeyAlias()
    {
        return tls.getKeyAlias();
    }

    public String getKeyStore()
    {
        return tls.getKeyStore();
    }

    public String getKeyStoreType()
    {
        return tls.getKeyStoreType();
    }

    public String getProtocolHandler()
    {
        return tls.getProtocolHandler();
    }

    public Provider getProvider()
    {
        return tls.getProvider();
    }

    public SecurityProviderFactory getSecurityProviderFactory()
    {
        return tls.getSecurityProviderFactory();
    }

    public String getSslType()
    {
        return tls.getSslType();
    }

    public String getKeyStorePassword()
    {
        return tls.getKeyStorePassword();
    }

    public String getTrustManagerAlgorithm()
    {
        return tls.getTrustManagerAlgorithm();
    }

    public TrustManagerFactory getTrustManagerFactory()
    {
        return tls.getTrustManagerFactory();
    }

    public String getTrustStore()
    {
        return tls.getTrustStore();
    }

    public String getTrustStorePassword()
    {
        return tls.getTrustStorePassword();
    }

    public String getTrustStoreType()
    {
        return tls.getTrustStoreType();
    }

    public boolean isExplicitTrustStoreOnly()
    {
        return tls.isExplicitTrustStoreOnly();
    }

    public boolean isRequireClientAuthentication()
    {
        return tls.isRequireClientAuthentication();
    }

    public void setClientKeyStore(String clientKeyStore) throws IOException
    {
        tls.setClientKeyStore(clientKeyStore);
    }

    public void setClientKeyStorePassword(String clientKeyStorePassword)
    {
        tls.setClientKeyStorePassword(clientKeyStorePassword);
    }

    public void setClientKeyStoreType(String clientKeyStoreType)
    {
        this.tls.setClientKeyStoreType(clientKeyStoreType);
    }

    public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly)
    {
        tls.setExplicitTrustStoreOnly(explicitTrustStoreOnly);
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        tls.setKeyManagerAlgorithm(keyManagerAlgorithm);
    }

    public void setKeyPassword(String keyPassword)
    {
        tls.setKeyPassword(keyPassword);
    }

    public void setKeyAlias(String alias)
    {
        tls.setKeyAlias(alias);
    }

    public void setKeyStore(String keyStore) throws IOException
    {
        tls.setKeyStore(keyStore);
    }

    public void setKeyStoreType(String keystoreType)
    {
        tls.setKeyStoreType(keystoreType);
    }

    public void setProtocolHandler(String protocolHandler)
    {
        tls.setProtocolHandler(protocolHandler);
    }

    public void setProvider(Provider provider)
    {
        tls.setProvider(provider);
    }

    public void setRequireClientAuthentication(boolean requireClientAuthentication)
    {
        tls.setRequireClientAuthentication(requireClientAuthentication);
    }

    public void setSecurityProviderFactory(SecurityProviderFactory spFactory)
    {
        tls.setSecurityProviderFactory(spFactory);
    }

    public void setSslType(String sslType)
    {
        tls.setSslType(sslType);
    }

    public void setKeyStorePassword(String storePassword)
    {
        tls.setKeyStorePassword(storePassword);
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        tls.setTrustManagerAlgorithm(trustManagerAlgorithm);
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory)
    {
        tls.setTrustManagerFactory(trustManagerFactory);
    }

    public void setTrustStore(String trustStore) throws IOException
    {
        tls.setTrustStore(trustStore);
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tls.setTrustStorePassword(trustStorePassword);
    }

    public void setTrustStoreType(String trustStoreType)
    {
        tls.setTrustStoreType(trustStoreType);
    }

    @Override
    protected AbstractConnector createJettyConnector()
    {
        SslSocketConnector cnn = new SslSocketConnector();

        if (SystemUtils.isIbmJDK())
        {
            cnn.setProtocol("SSL_TLS");
        }

        // get (from parent Mule connector) and set number of acceptor threads into the underlying SSL connector
        cnn.setAcceptors(getAcceptors());

        // set trust and keystore params
        if (tls.getKeyStore() != null) cnn.setKeystore(tls.getKeyStore());
        if (tls.getKeyPassword() != null) cnn.setKeyPassword(tls.getKeyPassword());
        if (tls.getKeyStoreType() != null) cnn.setKeystoreType(tls.getKeyStoreType());
        if (tls.getKeyManagerAlgorithm() != null) cnn.setSslKeyManagerFactoryAlgorithm(tls.getKeyManagerAlgorithm());
        if (tls.getProvider() != null) cnn.setProvider(tls.getProvider().getName());
        if (tls.getTrustStorePassword() != null) cnn.setTrustPassword(tls.getTrustStorePassword());
        if (tls.getTrustStore() != null) cnn.setTruststore(tls.getTrustStore());
        if (tls.getTrustStoreType() != null) cnn.setTruststoreType(tls.getTrustStoreType());
        if (tls.getTrustManagerAlgorithm() != null) cnn.setSslTrustManagerFactoryAlgorithm(tls.getTrustManagerAlgorithm());
        return cnn;
    }
}
