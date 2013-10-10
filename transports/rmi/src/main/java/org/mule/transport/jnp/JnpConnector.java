/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
