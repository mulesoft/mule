/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
