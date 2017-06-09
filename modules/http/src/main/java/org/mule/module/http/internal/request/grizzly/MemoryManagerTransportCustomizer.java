/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

public class MemoryManagerTransportCustomizer implements TransportCustomizer
{

    @Override
    public void customize(TCPNIOTransport tcpnioTransport, FilterChainBuilder filterChainBuilder)
    {
        tcpnioTransport.setMemoryManager(new HeapMemoryManager());
    }

}
