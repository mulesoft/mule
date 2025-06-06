/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static java.net.InetSocketAddress.createUnresolved;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.sdk.api.http.domain.message.request.ClientConnection;
import org.mule.sdk.api.http.domain.message.request.HttpRequestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseClientConfigImplTestCase {

  private SseClientConfigImpl configurer;

  @BeforeEach
  void setUp() {
    configurer = new SseClientConfigImpl();
  }

  @Test
  void withRequestContext() {
    configurer.withRequestContext(null);
    var sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getRemoteHostAddress(), is(nullValue()));

    var requestContext = mock(HttpRequestContext.class);
    configurer.withRequestContext(requestContext);
    sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getRemoteHostAddress(), is(nullValue()));

    var clientConnection = mock(ClientConnection.class);
    when(requestContext.getClientConnection()).thenReturn(clientConnection);
    configurer.withRequestContext(requestContext);
    sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getRemoteHostAddress(), is(nullValue()));

    var address = createUnresolved("localhost", 1234);
    when(clientConnection.getRemoteHostAddress()).thenReturn(address);
    configurer.withRequestContext(requestContext);
    sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getRemoteHostAddress(), is(address));
  }

  @Test
  void withClientId() {
    configurer.withClientId("18");
    var sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getClientId(), is("18"));
  }

  @Test
  void withRemoteAddress() {
    var address = createUnresolved("localhost", 1234);
    configurer.withRemoteAddress(address);
    var sseClientConfig = configurer.build();
    assertThat(sseClientConfig.getRemoteHostAddress(), is(address));
  }
}
