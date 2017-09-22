/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.properties;

import java.io.Serializable;

import javax.net.ssl.SSLSession;

/**
 * Wrapper to contain SSL context and avoid errors due to serialization
 *
 */
public class SSLSessionProperties implements Serializable
{

    private static final long serialVersionUID = -3468107642014943429L;
    
    private transient SSLSession session;

    public SSLSessionProperties(SSLSession session)
    {
        this.session = session;
    }

    public SSLSession retrieveSession()
    {
        return session;
    }
    
    public String getProtocol()
    {
        return session.getProtocol();
    }
    
    public String getCipherSuite()
    {
        return session.getCipherSuite();
    }

    public void setSession(SSLSession context)
    {
        this.session = context;
    }
}
