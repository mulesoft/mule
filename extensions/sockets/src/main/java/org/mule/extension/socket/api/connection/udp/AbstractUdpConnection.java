/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.udp;

import static java.lang.String.format;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.connection.AbstractSocketConnection;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.serialization.DefaultObjectSerializer;
import org.mule.runtime.core.api.serialization.ObjectSerializer;

import java.net.DatagramSocket;

import javax.inject.Inject;

/**
 * Provides fields and behaviour common to UDP connections
 */
public abstract class AbstractUdpConnection extends AbstractSocketConnection {

  protected final UdpSocketProperties socketProperties;
  protected DatagramSocket socket;

  @DefaultObjectSerializer
  @Inject
  protected ObjectSerializer objectSerializer;

  public AbstractUdpConnection(ConnectionSettings connectionSettings, UdpSocketProperties socketProperties)
      throws ConnectionException {
    super(connectionSettings);
    this.socketProperties = socketProperties;
  }

  @Override
  protected void doDisconnect() {
    socket.close();
  }

  @Override
  public ConnectionValidationResult validate() {
    if (socket.isClosed()) {
      return ConnectionValidationResult.failure("UDP socket was closed", ConnectionExceptionCode.UNKNOWN, null);
    }

    return ConnectionValidationResult.success();
  }

  protected DatagramSocket newSocket(ConnectionSettings connectionSettings) throws ConnectionException {
    try {
      return new DatagramSocket(connectionSettings.getInetSocketAddress());
    } catch (Exception e) {
      throw new ConnectionException(format("Could not bind UDP Socket to address %s", connectionSettings.toString(), e));
    }
  }
}
