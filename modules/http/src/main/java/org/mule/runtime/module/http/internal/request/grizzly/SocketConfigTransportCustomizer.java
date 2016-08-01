/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request.grizzly;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.mule.compatibility.transport.socket.api.TcpClientSocketProperties;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

/**
 * Transport customizer that configures socket properties.
 */
public class SocketConfigTransportCustomizer implements TransportCustomizer
{
    private final TcpClientSocketProperties clientSocketProperties;

    public SocketConfigTransportCustomizer(TcpClientSocketProperties clientSocketProperties)
    {
        this.clientSocketProperties = clientSocketProperties;
    }


    @Override
    public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder)
    {
        if (clientSocketProperties.getKeepAlive() != null)
        {
            transport.setKeepAlive(clientSocketProperties.getKeepAlive());
        }
        if (clientSocketProperties.getSendTcpNoDelay() != null)
        {
            transport.setTcpNoDelay(clientSocketProperties.getSendTcpNoDelay());
        }
        if (clientSocketProperties.getConnectionTimeout() != null)
        {
            transport.setConnectionTimeout(clientSocketProperties.getConnectionTimeout());
        }
        if (clientSocketProperties.getReceiveBufferSize() != null)
        {
            transport.setReadBufferSize(clientSocketProperties.getReceiveBufferSize());
        }
        if (clientSocketProperties.getSendBufferSize() != null)
        {
            transport.setWriteBufferSize(clientSocketProperties.getSendBufferSize());
        }
        if (clientSocketProperties.getTimeout() != null)
        {
            transport.setClientSocketSoTimeout(clientSocketProperties.getTimeout());
        }
        if (clientSocketProperties.getLinger() != null)
        {
            transport.setLinger(clientSocketProperties.getLinger());
        }
    }
}
