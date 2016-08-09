/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.client;

import static java.util.Arrays.copyOf;
import static org.mule.extension.socket.internal.SocketUtils.createPacket;
import static org.mule.extension.socket.internal.SocketUtils.getUdpAllowedByteArray;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.extension.socket.api.exceptions.ReadingTimeoutException;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.runtime.core.api.serialization.ObjectSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * This {@link SocketClient} implementation allows the reading and writing to and from a specific UDP {@link DatagramSocket}.
 */
public final class UdpClient implements SocketClient {

  private final UdpSocketProperties socketProperties;
  private final ObjectSerializer objectSerializer;
  private final DatagramSocket socket;
  private final SocketAddress socketAddress;

  public UdpClient(DatagramSocket socket, ConnectionSettings connectionSettings, UdpSocketProperties socketProperties,
                   ObjectSerializer objectSerializer) {
    this.objectSerializer = objectSerializer;
    this.socketProperties = socketProperties;
    this.socket = socket;
    this.socketAddress = connectionSettings.getInetSocketAddress();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(Object data, String outputEncoding) throws IOException {
    byte[] byteArray = getUdpAllowedByteArray(data, outputEncoding, objectSerializer);
    DatagramPacket sendPacket = createPacket(byteArray);
    sendPacket.setSocketAddress(socketAddress);
    socket.send(sendPacket);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read() throws IOException {
    DatagramPacket receivedPacket = createPacket(socketProperties.getReceiveBufferSize());
    receivedPacket.setSocketAddress(socketAddress);
    try {
      socket.receive(receivedPacket);
      return new ByteArrayInputStream(copyOf(receivedPacket.getData(), receivedPacket.getLength()));
    } catch (SocketTimeoutException e) {
      throw new ReadingTimeoutException("UDP socket timed out while waiting for a response", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    socket.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SocketAttributes getAttributes() {
    return new ImmutableSocketAttributes(socket);

  }
}
