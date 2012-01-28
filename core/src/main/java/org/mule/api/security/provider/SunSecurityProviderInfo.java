/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
