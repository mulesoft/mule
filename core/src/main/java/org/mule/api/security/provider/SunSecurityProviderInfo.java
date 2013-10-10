/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security.provider;

/**
 * Sun JDK-specific security provider information.
 */
public class SunSecurityProviderInfo implements SecurityProviderInfo
{
    private static final String KEY_MANAGER_ALGORITHM = "SunX509";
    private static final String PROTOCOL_HANDLER = "com.sun.net.ssl.internal.www.protocol";
    private static final String PROVIDER_CLASS = "com.sun.net.ssl.internal.ssl.Provider";
    private static final String DEFAULT_SSL_TYPE = "SSLv3";

    @Override
    public String getKeyManagerAlgorithm()
    {
        return KEY_MANAGER_ALGORITHM;
    }

    @Override
    public String getProtocolHandler()
    {
        return PROTOCOL_HANDLER;
    }

    @Override
    public String getProviderClass()
    {
        return PROVIDER_CLASS;
    }

    @Override
    public String getDefaultSslType()
    {
        return DEFAULT_SSL_TYPE;
    }
}
