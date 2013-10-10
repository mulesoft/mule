/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.MuleContext;

/**
 * <code>TlsConnector</code> Provides TLS connections
 */
public class TlsConnector extends SslConnector
{

    public static final String TLS = "tls";

    public TlsConnector(MuleContext context)
    {
        super(context);
    }
    
    public String getProtocol()
    {
        return TLS;
    }

}
