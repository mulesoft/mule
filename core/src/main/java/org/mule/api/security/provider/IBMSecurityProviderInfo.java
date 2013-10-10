/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.provider;

/**
 * IBM JDK-specific security provider information.
 */
public class IBMSecurityProviderInfo implements SecurityProviderInfo
{

    private static final String KEY_MANAGER_ALGORITHM = "IbmX509";
    private static final String PROTOCOL_HANDLER = "com.ibm.net.ssl.internal.www.protocol";
    private static final String PROVIDER_CLASS = "com.ibm.jsse.IBMJSSEProvider";
    private static final String DEFAULT_SSL_TYPE = "SSL_TLS";

    public String getKeyManagerAlgorithm()
    {
        return KEY_MANAGER_ALGORITHM;
    }

    public String getProtocolHandler()
    {
        return PROTOCOL_HANDLER;
    }

    public String getProviderClass()
    {
        return PROVIDER_CLASS;
    }

    public String getDefaultSslType()
    {
        return DEFAULT_SSL_TYPE;
    }

}
