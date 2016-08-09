/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp;

import static java.lang.String.format;
import static org.mule.extension.socket.internal.SocketUtils.configureConnection;

import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.connection.ListenerConnection;
import org.mule.extension.socket.api.connection.udp.UdpListenerConnection;
import org.mule.extension.socket.api.socket.factory.SimpleServerSocketFactory;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpSocketProperties;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.extension.socket.api.worker.TcpWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.runtime.MessageHandler;
import org.mule.runtime.extension.api.runtime.source.Source;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Implementation of {@link ListenerConnection} for receiving TCP connections.
 *
 * @since 4.0
 */
public final class TcpListenerConnection extends AbstractTcpConnection implements ListenerConnection {

  private final TcpServerSocketProperties socketProperties;
  private final SimpleServerSocketFactory serverSocketFactory;
  private ServerSocket serverSocket;

  public TcpListenerConnection(ConnectionSettings connectionSettings, TcpProtocol protocol,
                               TcpServerSocketProperties socketProperties, SimpleServerSocketFactory serverSocketFactory)
      throws ConnectionException {
    super(connectionSettings, protocol);
    this.socketProperties = socketProperties;
    this.serverSocketFactory = serverSocketFactory;
  }

  /**
   * One worker is created per accepted connection.
   * <p>
   * The new {@link Socket} used for responding has the same {@link TcpSocketProperties} settings as the listener socket used in
   * the {@link Source}.
   *
   * @throws ConnectionException if the socket was close while blocked on {@link DatagramSocket#receive(DatagramPacket)} method
   *         and the close was not intentionally done by the {@link UdpListenerConnection#disconnect()} method.
   * @throws IOException if the socket was close while blocked on {@link DatagramSocket#receive(DatagramPacket)} method.
   */
  @Override
  public SocketWorker listen(MessageHandler<InputStream, SocketAttributes> messageHandler)
      throws IOException, ConnectionException {
    Socket newConnection = acceptConnection();
    configureConnection(newConnection, socketProperties);
    return new TcpWorker(newConnection, protocol, messageHandler);
  }

  @Override
  public void doDisconnect() {
    try {
      serverSocket.close();
    } catch (IOException e) {
      LOGGER.error("An error occurred when closing TCP listener socket", e);
    }
  }

  /**
   * Configures the {@link ServerSocket} instance with the {@link TcpServerSocketProperties} parameters. It does not invoke
   * {@link ServerSocket#accept()} because that would block the method causing an incompatible behaviour with the {@link Source}
   * that uses this connection.
   */
  @Override
  public void connect() throws ConnectionException {
    try {
      serverSocket = serverSocketFactory.createServerSocket();

      if (socketProperties.getReceiveBufferSize() != null) {
        serverSocket.setReceiveBufferSize(socketProperties.getReceiveBufferSize());
      }

      if (socketProperties.getServerTimeout() != null) {
        serverSocket.setSoTimeout(socketProperties.getServerTimeout());
      }

      serverSocket.setReuseAddress(socketProperties.getReuseAddress());
    } catch (Exception e) {
      throw new ConnectionException("Could not create TCP listener socket", e);
    }

    InetSocketAddress address = getSocketAddress(connectionSettings, socketProperties.getFailOnUnresolvedHost());

    try {
      serverSocket.bind(address, socketProperties.getReceiveBacklog());
    } catch (IOException e) {
      throw new ConnectionException(format("Could not bind socket to host '%s' and port '%d'", connectionSettings.getHost(),
                                           connectionSettings.getPort()),
                                    e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate() {
    if (serverSocket.isClosed()) {
      return ConnectionValidationResult.failure("TCP server socket was closed", ConnectionExceptionCode.UNKNOWN, null);
    } else if (!serverSocket.isBound()) {
      return ConnectionValidationResult.failure("TCP server socket was not bounded", ConnectionExceptionCode.UNKNOWN, null);

    }

    return ConnectionValidationResult.success();
  }

  /**
   * @return a {@link Socket} from a received connection
   * @throws ConnectionException if the socket was closed by a different means than invoking
   *         {@link TcpListenerConnection#disconnect}
   * @throws IOException if the socket was closed while blocked in {@link ServerSocket#accept()}
   * @throws SocketTimeoutException if {@link TcpServerSocketProperties#getServerTimeout()} is reached.
   */
  private Socket acceptConnection() throws ConnectionException, IOException {
    try {
      return serverSocket.accept();
    } catch (IOException e) {
      if (!wasDisconnected) {
        throw new ConnectionException("An error occurred while listening for new TCP connections", e);
      }

      // socket was not intentionally closed by the connection provider
      LOGGER.debug("TCP listener socket has been gracefully closed");

      throw e;
    }
  }
}
