/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.grizzly;


import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

/**
 * Transport customizer that configures socket properties.
 */
public class SocketConfigTransportCustomizer implements TransportCustomizer {

  private final TcpClientSocketProperties clientSocketProperties;

  public SocketConfigTransportCustomizer(TcpClientSocketProperties clientSocketProperties) {
    this.clientSocketProperties = clientSocketProperties;
  }


  @Override
  public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder) {
    if (clientSocketProperties.getKeepAlive() != null) {
      transport.setKeepAlive(clientSocketProperties.getKeepAlive());
    }

    if (clientSocketProperties.getReceiveBufferSize() != null) {
      transport.setReadBufferSize(clientSocketProperties.getReceiveBufferSize());
    }

    if (clientSocketProperties.getSendBufferSize() != null) {
      transport.setWriteBufferSize(clientSocketProperties.getSendBufferSize());
    }

    if (clientSocketProperties.getClientTimeout() != null) {
      transport.setClientSocketSoTimeout(clientSocketProperties.getClientTimeout());
    }

    if (clientSocketProperties.getLinger() != null) {
      transport.setLinger(clientSocketProperties.getLinger());
    }

    transport.setTcpNoDelay(clientSocketProperties.getSendTcpNoDelay());
    transport.setConnectionTimeout(clientSocketProperties.getConnectionTimeout());
  }
}
