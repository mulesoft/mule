/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.client;

import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link SocketClient} implementation allows the reading and writing to and from a specific connected TCP {@link Socket}.
 */
public final class TcpClient implements SocketClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(TcpClient.class);
  private final Socket socket;
  private final TcpProtocol protocol;

  public TcpClient(Socket socket, TcpProtocol protocol) {
    this.socket = socket;
    this.protocol = protocol;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(Object data, String outputEncoding) throws IOException {
    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
    protocol.write(bufferedOutputStream, data, outputEncoding);
    bufferedOutputStream.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read() throws IOException {
    InputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    return protocol.read(inputStream);
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
