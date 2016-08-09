/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.udp;

import static org.mule.extension.socket.internal.SocketUtils.configureConnection;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.client.SocketClient;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.extension.socket.api.client.UdpClient;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;

import java.net.DatagramSocket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RequesterConnection} for UDP connections.
 */
@Alias("udp-request-connection")
public class UdpRequesterConnection extends AbstractUdpConnection implements RequesterConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(UdpRequesterConnection.class);
  private final ConnectionSettings localAddressSettings;

  public UdpRequesterConnection(ConnectionSettings connectionSettings, ConnectionSettings localAddressSettings,
                                UdpSocketProperties socketProperties)
      throws ConnectionException {
    super(connectionSettings, socketProperties);
    this.localAddressSettings = localAddressSettings;
  }

  /**
   * Configures a {@link DatagramSocket} with the settings provided in the {@link UdpSocketProperties} and bounded to the
   * connection settings presents on {@code localAddressSettings}.
   * <p>
   * It does not invoke {@link DatagramSocket#connect(SocketAddress)}, because that will only allow the socket to receive packages
   * from the {@link SocketAddress} it has connected to.
   *
   * @throws ConnectionException if the configuration of the {@link DatagramSocket} fails
   */
  @Override
  public void connect() throws ConnectionException {
    socket = newSocket(localAddressSettings);
    configureConnection(socket, socketProperties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SocketClient getClient() {
    return new UdpClient(socket, connectionSettings, socketProperties, objectSerializer);
  }

}
