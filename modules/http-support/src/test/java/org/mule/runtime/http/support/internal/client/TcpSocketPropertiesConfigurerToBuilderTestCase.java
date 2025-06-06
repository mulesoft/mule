/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.http.api.tcp.TcpClientSocketProperties;
import org.mule.runtime.http.api.tcp.TcpClientSocketPropertiesBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TcpSocketPropertiesConfigurerToBuilderTestCase {

  private TcpSocketPropertiesConfigurerToBuilder configurer;

  private TcpClientSocketPropertiesBuilder actualBuilder;

  @BeforeEach
  void setUp() {
    actualBuilder = TcpClientSocketProperties.builder();
    configurer = new TcpSocketPropertiesConfigurerToBuilder(actualBuilder);
  }

  @Test
  void sendBufferSize() {
    configurer.sendBufferSize(123);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getSendBufferSize(), is(123));
  }

  @Test
  void receiveBufferSize() {
    configurer.receiveBufferSize(123);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getReceiveBufferSize(), is(123));
  }

  @Test
  void clientTimeout() {
    configurer.clientTimeout(123);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getClientTimeout(), is(123));
  }

  @Test
  void sendTcpNoDelay() {
    configurer.sendTcpNoDelay(true);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getSendTcpNoDelay(), is(true));
  }

  @Test
  void linger() {
    configurer.linger(12);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getLinger(), is(12));
  }

  @Test
  void keepAlive() {
    configurer.keepAlive(false);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getKeepAlive(), is(false));
  }

  @Test
  void connectionTimeout() {
    configurer.connectionTimeout(123);
    TcpClientSocketProperties properties = actualBuilder.build();
    assertThat(properties.getConnectionTimeout(), is(123));
  }
}
