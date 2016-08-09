/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.udp;

import static org.mule.extension.socket.internal.SocketUtils.configureConnection;
import static org.mule.extension.socket.internal.SocketUtils.createPacket;

import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.connection.ListenerConnection;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.extension.socket.api.worker.UdpWorker;
import org.mule.extension.socket.api.exceptions.ReadingTimeoutException;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.runtime.MessageHandler;
import org.mule.runtime.extension.api.runtime.source.Source;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class UdpListenerConnection extends AbstractUdpConnection implements ListenerConnection {

  public UdpListenerConnection(ConnectionSettings connectionSettings, UdpSocketProperties socketProperties)
      throws ConnectionException {
    super(connectionSettings, socketProperties);
  }

  @Override
  public void connect() throws ConnectionException {
    socket = newSocket(connectionSettings);
    configureConnection(socket, socketProperties);
  }

  /**
   * One worker is created per received package. If the other end of the connection is awaiting for a response, one will be sent
   * but not from the same listener socket the source has. The response will be sent from a new different {@link DatagramSocket}
   * bound to a port choose by the system.
   * <p>
   * The new {@link DatagramSocket} used for responding has the same configuration settings than the listener socket used in the
   * {@link Source}.
   *
   * @throws ReadingTimeoutException if the socket reached timeout while awaiting the arrival of a new package.
   * @throws ConnectionException if the socket was close while blocked on {@link DatagramSocket#receive(DatagramPacket)} method
   *         and the close was not intentionally done by the {@link UdpListenerConnection#disconnect()} method.
   * @throws IOException if the socket was close while blocked on {@link DatagramSocket#receive(DatagramPacket)} method.
   */
  @Override
  public SocketWorker listen(MessageHandler<InputStream, SocketAttributes> messageHandler)
      throws IOException, ConnectionException {
    DatagramPacket packet = createPacket(socketProperties.getReceiveBufferSize());

    try {
      socket.receive(packet);
    } catch (SocketTimeoutException e) {
      throw new ReadingTimeoutException("UDP Source timed out while awaiting for new packages", e);
    } catch (IOException e) {
      if (!wasDisconnected) {
        throw new ConnectionException("An error occurred while listening for new UDP packets", e);
      }

      // socket was not intentionally closed by the connection provider
      LOGGER.debug("UDP listener socket has been gracefully closed");

      throw e;
    }

    DatagramSocket newConnection = new DatagramSocket();
    configureConnection(newConnection, socketProperties);
    return new UdpWorker(newConnection, packet, objectSerializer, messageHandler);
  }
}
