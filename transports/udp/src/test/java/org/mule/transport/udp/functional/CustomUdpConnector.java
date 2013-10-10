/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp.functional;

import org.mule.api.MuleContext;
import org.mule.transport.ConfigurableKeyedObjectPool;
import org.mule.transport.udp.UdpConnector;

/**
 * Allows access to the dispatchers pool so we can ensure they're disposed
 */
public class CustomUdpConnector extends UdpConnector
{
    public CustomUdpConnector(MuleContext context)
    {
        super(context);
    }
    
    public ConfigurableKeyedObjectPool getDispatchers() 
    {
        return dispatchers;
    }
}


