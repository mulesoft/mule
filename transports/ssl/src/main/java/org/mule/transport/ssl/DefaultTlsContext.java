/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;


import java.io.IOException;

/**
 * TLS configuration for key store and trust store.
 */
public class DefaultTlsContext implements TlsContext
{
    private String name;

    private String keyStorePath;
    private String keyStoreType = "JKS";
    private transient String keyStorePassword;
    private transient String keyManagerPassword;
    private String keyManagerAlgorithm;

    private String trustStorePath;
    private String trustStoreType = "JKS";
    private transient String trustStorePassword;
    private String trustManagerAlgorithm;


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getKeyStorePath()
    {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) throws IOException
    {
        this.keyStorePath = keyStorePath;
    }

    @Override
    public String getKeyStoreType()
    {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType)
    {
        this.keyStoreType = keyStoreType;
    }

    @Override
    public String getKeyStorePassword()
    {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword)
    {
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public String getKeyManagerPassword()
    {
        return keyManagerPassword;
    }

    public void setKeyManagerPassword(String keyManagerPassword)
    {
        this.keyManagerPassword = keyManagerPassword;
    }

    @Override
    public String getKeyManagerAlgorithm()
    {
        return keyManagerAlgorithm;
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        this.keyManagerAlgorithm = keyManagerAlgorithm;
    }

    @Override
    public String getTrustManagerAlgorithm()
    {
        return trustManagerAlgorithm;
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        this.trustManagerAlgorithm = trustManagerAlgorithm;
    }

    @Override
    public String getTrustStorePath()
    {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) throws IOException
    {
        this.trustStorePath = trustStorePath;
    }

    @Override
    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType)
    {
        this.trustStoreType = trustStoreType;
    }

    @Override
    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        this.trustStorePassword = trustStorePassword;
    }


}
