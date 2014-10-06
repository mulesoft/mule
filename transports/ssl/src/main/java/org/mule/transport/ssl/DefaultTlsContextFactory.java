/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;


import org.mule.api.security.tls.TlsConfiguration;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Default implementation of the {@code TlsContextFactory} interface, which delegates all its operations to a
 * {@code TlsConfiguration} object.
 */
public class DefaultTlsContextFactory implements TlsContextFactory
{
    private String name;

    private TlsConfiguration tlsConfiguration = new TlsConfiguration(null);

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getKeyStorePath()
    {
        return tlsConfiguration.getKeyStore();
    }

    public void setKeyStorePath(String name) throws IOException
    {
        tlsConfiguration.setKeyStore(name);
    }

    public String getKeyStoreType()
    {
        return tlsConfiguration.getKeyStoreType();
    }

    public void setKeyStoreType(String keyStoreType)
    {
        tlsConfiguration.setKeyStoreType(keyStoreType);
    }

    public String getKeyStorePassword()
    {
        return tlsConfiguration.getKeyStorePassword();
    }

    public void setKeyStorePassword(String storePassword)
    {
        tlsConfiguration.setKeyStorePassword(storePassword);
    }

    public String getKeyManagerPassword()
    {
        return tlsConfiguration.getKeyPassword();
    }

    public void setKeyManagerPassword(String keyManagerPassword)
    {
        tlsConfiguration.setKeyPassword(keyManagerPassword);
    }

    public String getKeyManagerAlgorithm()
    {
        return tlsConfiguration.getKeyManagerAlgorithm();
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        tlsConfiguration.setKeyManagerAlgorithm(keyManagerAlgorithm);
    }

    public String getTrustStorePath()
    {
        return tlsConfiguration.getTrustStore();
    }

    public void setTrustStorePath(String trustStorePath) throws IOException
    {
        tlsConfiguration.setTrustStore(trustStorePath);
    }

    public String getTrustStoreType()
    {
        return tlsConfiguration.getTrustStoreType();
    }

    public void setTrustStoreType(String trustStoreType)
    {
        tlsConfiguration.setTrustStoreType(trustStoreType);
    }

    public String getTrustStorePassword()
    {
        return tlsConfiguration.getTrustStorePassword();
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tlsConfiguration.setTrustStorePassword(trustStorePassword);
    }

    public String getTrustManagerAlgorithm()
    {
        return tlsConfiguration.getTrustManagerAlgorithm();
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        tlsConfiguration.setTrustManagerAlgorithm(trustManagerAlgorithm);
    }

    @Override
    public SSLSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
        return tlsConfiguration.getSocketFactory();
    }

    @Override
    public SSLServerSocketFactory getServerSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
        return tlsConfiguration.getServerSocketFactory();
    }

}
