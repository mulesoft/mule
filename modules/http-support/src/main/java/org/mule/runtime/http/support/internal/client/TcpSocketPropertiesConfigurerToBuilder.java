/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.runtime.http.api.tcp.TcpClientSocketPropertiesBuilder;
import org.mule.sdk.api.http.tcp.TcpSocketPropertiesConfigurer;

public class TcpSocketPropertiesConfigurerToBuilder implements TcpSocketPropertiesConfigurer {

  private final TcpClientSocketPropertiesBuilder builder;

  TcpSocketPropertiesConfigurerToBuilder(TcpClientSocketPropertiesBuilder builder) {
    this.builder = builder;
  }

  @Override
  public TcpSocketPropertiesConfigurer sendBufferSize(Integer sendBufferSize) {
    builder.sendBufferSize(sendBufferSize);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer receiveBufferSize(Integer receiveBufferSize) {
    builder.receiveBufferSize(receiveBufferSize);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer clientTimeout(Integer clientTimeout) {
    builder.clientTimeout(clientTimeout);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer sendTcpNoDelay(Boolean sendTcpNoDelay) {
    builder.sendTcpNoDelay(sendTcpNoDelay);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer linger(Integer linger) {
    builder.linger(linger);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer keepAlive(Boolean keepAlive) {
    builder.keepAlive(keepAlive);
    return this;
  }

  @Override
  public TcpSocketPropertiesConfigurer connectionTimeout(Integer connectionTimeout) {
    builder.connectionTimeout(connectionTimeout);
    return this;
  }
}
