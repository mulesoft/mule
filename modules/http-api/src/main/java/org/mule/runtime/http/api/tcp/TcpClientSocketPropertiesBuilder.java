/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.tcp;

/**
 * Builder of {@link TcpClientSocketProperties}. Instances can only be obtained using {@link TcpClientSocketProperties#builder()}.
 * Default values will be used unless specified.
 *
 * @since 4.0
 */
public final class TcpClientSocketPropertiesBuilder {

  protected Integer sendBufferSize;
  protected Integer receiveBufferSize;
  protected Integer clientTimeout;
  protected Boolean sendTcpNoDelay = true;
  protected Integer linger;
  protected Boolean keepAlive = false;
  private Integer connectionTimeout = 30000;

  TcpClientSocketPropertiesBuilder() {}

  /**
   * Defines the size of the buffer to use when sending data. See {@link TcpSocketProperties#getSendBufferSize()}.
   * If not set, transport defaults will be used.
   *
   * @param sendBufferSize size of the buffer (in bytes).
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder sendBufferSize(Integer sendBufferSize) {
    this.sendBufferSize = sendBufferSize;
    return this;
  }

  /**
   * Defines the size of the buffer to use when receiving data. See {@link TcpSocketProperties#getReceiveBufferSize()}.
   * If not set, transport defaults will be used.
   *
   * @param receiveBufferSize size of the buffer (in bytes).
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder receiveBufferSize(Integer receiveBufferSize) {
    this.receiveBufferSize = receiveBufferSize;
    return this;
  }

  /**
   * Defines the SO_TIMEOUT value for client sockets. See {@link TcpSocketProperties#getClientTimeout()}.
   * If not set, transport defaults will be used.
   *
   * @param clientTimeout the timeout (in milliseconds).
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder clientTimeout(Integer clientTimeout) {
    this.clientTimeout = clientTimeout;
    return this;
  }

  /**
   * Defines whether data should be collected or not before sending. See {@link TcpSocketProperties#getSendTcpNoDelay()}.
   * Default value is true.
   *
   * @param sendTcpNoDelay whether to send everything or collect data
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder sendTcpNoDelay(Boolean sendTcpNoDelay) {
    this.sendTcpNoDelay = sendTcpNoDelay;
    return this;
  }

  /**
   * Defines the SO_LINGER value, related to how long the socket will take to close. See {@link TcpSocketProperties#getLinger()}.
   * If not set, transport defaults will be used.
   *
   * @param linger timeout (in milliseconds)
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder linger(Integer linger) {
    this.linger = linger;
    return this;
  }

  /**
   * Defines the SO_KEEPALIVE behaviour for open sockets. See {@link TcpSocketProperties#getKeepAlive()}.
   * Default value is true.
   *
   * @param keepAlive
   * @return
   */
  public TcpClientSocketPropertiesBuilder keepAlive(Boolean keepAlive) {
    this.keepAlive = keepAlive;
    return this;
  }

  /**
   * Defines how long to wait for the outbound connection to be created. See {@link TcpClientSocketProperties#getConnectionTimeout()}.
   * Default value is 30000.
   *
   * @param connectionTimeout timeout (in milliseconds)
   * @return this builder
   */
  public TcpClientSocketPropertiesBuilder connectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  /**
   * @return a {@link TcpClientSocketProperties} instance as specified.
   */
  public TcpClientSocketProperties build() {
    return new DefaultTcpClientSocketProperties(sendBufferSize, receiveBufferSize, clientTimeout, sendTcpNoDelay, linger,
                                                keepAlive, connectionTimeout);
  }
}
