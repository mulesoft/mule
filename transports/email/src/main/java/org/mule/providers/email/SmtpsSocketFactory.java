/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.umo.security.tls.TlsPropertiesSocketFactory;

import javax.net.SocketFactory;

/**
 * A socket factor that reads "indirect" configuration (see {@link org.mule.umo.security.tls.TlsConfiguration})
 * for SMTPS from System properties.
 */
public class SmtpsSocketFactory extends TlsPropertiesSocketFactory
{
    
    public static final String MULE_SMTPS_NAMESPACE = "mule.email.smtps";

    public SmtpsSocketFactory()
    {
        super(true, MULE_SMTPS_NAMESPACE);
    }
    
    public static SocketFactory getDefault() 
    {
        return new SmtpsSocketFactory();
    }

}


