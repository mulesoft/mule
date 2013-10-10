/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jnp;

import org.mule.api.MuleContext;
import org.mule.transport.rmi.RmiConnector;

/**
 * <code>JnpConnector</code> uses the Java Naming protocol to bind to remote
 * objects
 */
public class JnpConnector extends RmiConnector
{

    public static final String JNP = "jnp";

    public JnpConnector(MuleContext context)
    {
        super(context);
    }
    
    public String getProtocol()
    {
        return JNP;
    }

}
