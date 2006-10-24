/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security.provider;

/**
 * Sun JDK-specific security provider information.
 */
public class SunSecurityProviderInfo implements SecurityProviderInfo
{

    private static final String KEY_MANAGER_ALGORITHM = "SunX509";

    private static final String PROTOCOL_HANDLER = "com.sun.net.ssl.internal.www.protocol";

    private static final String PROVIDER_CLASS = "com.sun.net.ssl.internal.ssl.Provider";

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

}
