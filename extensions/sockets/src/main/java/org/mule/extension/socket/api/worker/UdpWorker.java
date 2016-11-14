/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.worker;

import static java.lang.String.format;
import static java.util.Arrays.copyOf;
import static org.mule.extension.socket.internal.SocketUtils.createPacket;
import static org.mule.extension.socket.internal.SocketUtils.getUdpAllowedByteArray;

import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One worker is created per received package. If the other end of the connection is awaiting for a response, one will be sent but
 * not from the same listener socket the source has. The response will be sent from a new different {@link DatagramSocket} bound
 * to a port choose by the system.
 */
public final class UdpWorker extends SocketWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(UdpWorker.class);
  private final DatagramSocket socket;
  private final DatagramPacket packet;
  private final ObjectSerializer objectSerializer;

  public UdpWorker(DatagramSocket socket,
                   DatagramPacket packet,
                   ObjectSerializer objectSerializer,
                   SourceCallback<InputStream, SocketAttributes> callback) {
    super(callback);
    this.socket = socket;
    this.packet = packet;
    this.objectSerializer = objectSerializer;
  }

  @Override
  public void doRun() {
    InputStream content = new ByteArrayInputStream(copyOf(packet.getData(), packet.getLength()));
    handle(content, new ImmutableSocketAttributes(packet));
  }

  @Override
  public void onComplete(Object result) {
    try {
      byte[] byteArray = getUdpAllowedByteArray(result, encoding, objectSerializer);
      DatagramPacket sendPacket = createPacket(byteArray);
      sendPacket.setSocketAddress(packet.getSocketAddress());
      socket.send(sendPacket);
    } catch (IOException e) {
      callback.onSourceException(new IOException(format("An error occurred while sending UDP packet to address '%s'",
                                                        packet.getSocketAddress().toString(), e)));
    }
  }

  @Override
  public void onError(Throwable e) {
    LOGGER.error("UDP worker will not answer back due an exception was received", e);
  }

  @Override
  public void dispose() {
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
      } catch (Exception e) {
        LOGGER.error("UDP worker failed closing socket", e);
      }
    }
  }
}
