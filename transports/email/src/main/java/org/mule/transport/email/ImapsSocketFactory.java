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
 * for IMAPS from System properties.
 */
public class ImapsSocketFactory extends TlsPropertiesSocketFactory
{

    public static final String MULE_IMAPS_NAMESPACE = "mule.email.imaps";

    public ImapsSocketFactory()
    {
        super(true, MULE_IMAPS_NAMESPACE);
    }
    
    public static SocketFactory getDefault() 
    {
        return new ImapsSocketFactory();
    }

}


