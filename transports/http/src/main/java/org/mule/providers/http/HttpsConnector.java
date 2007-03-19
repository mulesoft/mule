/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.TlsSupport;
import org.mule.umo.security.provider.SecurityProviderFactory;

import java.io.IOException;
import java.security.Provider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * <code>HttpsConnector</code> provides Https connectivity
 */
public class HttpsConnector extends HttpConnector
{
    private TlsSupport tlsSupport = new TlsSupport();

    protected void doInitialise() throws InitialisationException
    {
        tlsSupport.initialiseFactories(false);
        super.doInitialise();
        tlsSupport.initialiseStores();
    }

    public String getProtocol()
    {
        return "https";
    }

    public String getClientKeyStore()
    {
        return tlsSupport.getClientKeyStore();
    }

    public String getClientKeyStorePassword()
    {
        return tlsSupport.getClientKeyStorePassword();
    }

    public String getKeyManagerAlgorithm()
    {
        return tlsSupport.getKeyManagerAlgorithm();
    }

    public KeyManagerFactory getKeyManagerFactory()
    {
        return tlsSupport.getKeyManagerFactory();
    }

    public String getKeyPassword()
    {
        return tlsSupport.getKeyPassword();
    }

    public String getKeyStore()
    {
        return tlsSupport.getKeyStore();
    }

    public String getKeystoreType()
    {
        return tlsSupport.getKeystoreType();
    }

    public String getProtocolHandler()
    {
        return tlsSupport.getProtocolHandler();
    }

    public Provider getProvider()
    {
        return tlsSupport.getProvider();
    }

    public SecurityProviderFactory getSecurityProviderFactory()
    {
        return tlsSupport.getSecurityProviderFactory();
    }

    public String getSslType()
    {
        return tlsSupport.getSslType();
    }

    public String getStorePassword()
    {
        return tlsSupport.getStorePassword();
    }

    public String getTrustManagerAlgorithm()
    {
        return tlsSupport.getTrustManagerAlgorithm();
    }

    public TrustManagerFactory getTrustManagerFactory()
    {
        return tlsSupport.getTrustManagerFactory();
    }

    public String getTrustStore()
    {
        return tlsSupport.getTrustStore();
    }

    public String getTrustStorePassword()
    {
        return tlsSupport.getTrustStorePassword();
    }

    public String getTrustStoreType()
    {
        return tlsSupport.getTrustStoreType();
    }

    public boolean isExplicitTrustStoreOnly()
    {
        return tlsSupport.isExplicitTrustStoreOnly();
    }

    public boolean isRequireClientAuthentication()
    {
        return tlsSupport.isRequireClientAuthentication();
    }

    public void setClientKeyStore(String clientKeyStore) throws IOException
    {
        tlsSupport.setClientKeyStore(clientKeyStore);
    }

    public void setClientKeyStorePassword(String clientKeyStorePassword)
    {
        tlsSupport.setClientKeyStorePassword(clientKeyStorePassword);
    }

    public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly)
    {
        tlsSupport.setExplicitTrustStoreOnly(explicitTrustStoreOnly);
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        tlsSupport.setKeyManagerAlgorithm(keyManagerAlgorithm);
    }

    public void setKeyPassword(String keyPassword)
    {
        tlsSupport.setKeyPassword(keyPassword);
    }

    public void setKeyStore(String keyStore)
    {
        tlsSupport.setKeyStore(keyStore);
    }

    public void setKeystoreType(String keystoreType)
    {
        tlsSupport.setKeystoreType(keystoreType);
    }

    public void setProtocolHandler(String protocolHandler)
    {
        tlsSupport.setProtocolHandler(protocolHandler);
    }

    public void setProvider(Provider provider)
    {
        tlsSupport.setProvider(provider);
    }

    public void setRequireClientAuthentication(boolean requireClientAuthentication)
    {
        tlsSupport.setRequireClientAuthentication(requireClientAuthentication);
    }

    public void setSecurityProviderFactory(SecurityProviderFactory spFactory)
    {
        tlsSupport.setSecurityProviderFactory(spFactory);
    }

    public void setSslType(String sslType)
    {
        tlsSupport.setSslType(sslType);
    }

    public void setStorePassword(String storePassword)
    {
        tlsSupport.setStorePassword(storePassword);
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        tlsSupport.setTrustManagerAlgorithm(trustManagerAlgorithm);
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory)
    {
        tlsSupport.setTrustManagerFactory(trustManagerFactory);
    }

    public void setTrustStore(String trustStore) throws IOException
    {
        tlsSupport.setTrustStore(trustStore);
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tlsSupport.setTrustStorePassword(trustStorePassword);
    }

    public void setTrustStoreType(String trustStoreType)
    {
        tlsSupport.setTrustStoreType(trustStoreType);
    }

}
