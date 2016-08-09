/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.provider.udp;

import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.connection.udp.UdpRequesterConnection;
import org.mule.extension.socket.api.exceptions.UnresolvableHostException;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.extension.socket.internal.SocketUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.net.DatagramSocket;

/**
 * A {@link ConnectionProvider} which provides instances of {@link UdpRequesterConnection} to be used by the
 * {@link SocketOperations}
 *
 * @since 4.0
 */
@Alias("udp-requester")
public final class UdpRequesterProvider implements PoolingConnectionProvider<UdpRequesterConnection> {

  /**
   * This configuration parameter refers to the address where the {@link DatagramSocket} should send packages to.
   */
  @ParameterGroup
  private ConnectionSettings connectionSettings;

  /**
   * {@link DatagramSocket} configuration properties
   */
  @ParameterGroup
  private UdpSocketProperties udpSocketProperties;

  /**
   * This configuration parameter refers to the address where the {@link DatagramSocket} should bind to.
   */
  @Parameter
  @Optional
  private ConnectionSettings localAddressSettings = new ConnectionSettings();

  @Override
  public UdpRequesterConnection connect() throws ConnectionException, UnresolvableHostException {
    UdpRequesterConnection connection = new UdpRequesterConnection(connectionSettings, localAddressSettings, udpSocketProperties);
    connection.connect();
    return connection;
  }

  @Override
  public void disconnect(UdpRequesterConnection connection) {
    connection.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(UdpRequesterConnection connection) {
    return SocketUtils.validate(connection);
  }
}

