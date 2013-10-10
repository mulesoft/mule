/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


