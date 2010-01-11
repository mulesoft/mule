/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security.provider;


/**
 * IBM JDK 1.4.2-specific security provider information. Contains config details for
 * IBM JSSE2. Note, versions lower than 1.4.1 <strong>do not</strong> have this
 * provider bundled.
 */
public class IBMSecurityProvider2Info extends IBMSecurityProviderInfo
{

    private static final String PROTOCOL_HANDLER = "com.ibm.net.ssl.www2.protocol.Handler";
    private static final String PROVIDER_CLASS = "com.ibm.jsse2.IBMJSSEProvider2";

    @Override
    public String getProtocolHandler()
    {
        return IBMSecurityProvider2Info.PROTOCOL_HANDLER;
    }

    @Override
    public String getProviderClass()
    {
        return IBMSecurityProvider2Info.PROVIDER_CLASS;
    }

}
