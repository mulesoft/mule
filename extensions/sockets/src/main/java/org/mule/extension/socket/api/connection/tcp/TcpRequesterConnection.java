/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp;

import static java.lang.String.format;
import static org.mule.extension.socket.internal.SocketUtils.configureConnection;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.extension.socket.api.socket.factory.SimpleSocketFactory;
import org.mule.extension.socket.api.client.TcpClient;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.io.IOException;
import java.net.Socket;

/**
 * Implementation of {@link RequesterConnection} for establishing TCP connections.
 *
 * @since 4.0
 */
public class TcpRequesterConnection extends AbstractTcpConnection implements RequesterConnection {

  private Socket socket;
  private final TcpClientSocketProperties socketProperties;
  private final ConnectionSettings localAddressSettings;
  private final SimpleSocketFactory socketFactory;

  public TcpRequesterConnection(ConnectionSettings connectionSettings, ConnectionSettings localAddressSettings,
                                TcpProtocol protocol, TcpClientSocketProperties socketProperties,
                                SimpleSocketFactory socketFactory)
      throws ConnectionException {
    super(connectionSettings, protocol);
    this.socketProperties = socketProperties;
    this.socketFactory = socketFactory;
    this.localAddressSettings = localAddressSettings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TcpClient getClient() {
    return new TcpClient(socket, protocol);
  }

  @Override
  public void doDisconnect() {
    try {
      socket.close();
    } catch (IOException e) {
      LOGGER.error("An error occurred when  closing TCP requester socket", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connect() throws ConnectionException {
    try {
      socket = socketFactory.createSocket();
      configureConnection(socket, socketProperties);
      socket.bind(localAddressSettings.getInetSocketAddress());
      socket.connect(getSocketAddress(connectionSettings, socketProperties.getFailOnUnresolvedHost()),
                     socketProperties.getConnectionTimeout());
    } catch (Exception e) {
      throw new ConnectionException(format("Could not connect TCP requester socket to host '%s' on port '%d'",
                                           connectionSettings.getHost(), connectionSettings.getPort()),
                                    e);
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate() {

    if (!socket.isBound()) {
      return ConnectionValidationResult.failure("TCP client socket was not bounded", ConnectionExceptionCode.UNKNOWN, null);

    } else if (!socket.isConnected()) {
      return ConnectionValidationResult.failure("TCP client socket was not connected", ConnectionExceptionCode.UNKNOWN, null);
    } else if (socket.isClosed()) {
      return ConnectionValidationResult.failure("TCP client socket was closed", ConnectionExceptionCode.UNKNOWN, null);
    }

    return ConnectionValidationResult.success();
  }
}
