/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import org.mule.runtime.core.api.MuleContext;

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
    
    @Override
    public String getProtocol()
    {
        return TLS;
    }

}
