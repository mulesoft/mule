/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.provider.udp;

import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.connection.udp.UdpListenerConnection;
import org.mule.extension.socket.api.exceptions.UnresolvableHostException;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.extension.socket.api.source.SocketListener;
import org.mule.extension.socket.internal.SocketUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ParameterGroup;

import java.net.DatagramSocket;


/**
 * A {@link ConnectionProvider} which provides instances of {@link UdpListenerConnection} to be used by {@link SocketListener}
 *
 * @since 4.0
 */
@Alias("udp-listener")
public class UdpListenerProvider implements ConnectionProvider<UdpListenerConnection> {

  /**
   * This configuration parameter refers to the address where the UDP socket should listen for incoming packets.
   */
  @ParameterGroup
  private ConnectionSettings connectionSettings;

  /**
   * {@link DatagramSocket} configuration properties
   */
  @ParameterGroup
  private UdpSocketProperties udpSocketProperties;

  @Override
  public UdpListenerConnection connect() throws ConnectionException, UnresolvableHostException {
    UdpListenerConnection connection = new UdpListenerConnection(connectionSettings, udpSocketProperties);
    connection.connect();
    return connection;
  }

  @Override
  public void disconnect(UdpListenerConnection connection) {
    connection.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(UdpListenerConnection connection) {
    return SocketUtils.validate(connection);
  }
}
