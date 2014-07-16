/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.security.tls.TlsPropertiesSocketFactory;

import javax.net.SocketFactory;

/**
 * A socket factor that reads "indirect" configuration (see {@link org.mule.api.security.tls.TlsConfiguration})
 * for POP3S from System properties.
 */
public class Pop3sSocketFactory extends TlsPropertiesSocketFactory
{

    public static final String MULE_POP3S_NAMESPACE = "mule.email.pop3s";

    public Pop3sSocketFactory()
    {
        super(true, MULE_POP3S_NAMESPACE);
    }
    
    public static SocketFactory getDefault() 
    {
        return new Pop3sSocketFactory();
    }

}


