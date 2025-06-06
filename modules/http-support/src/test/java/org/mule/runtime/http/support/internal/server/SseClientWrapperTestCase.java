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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.sse.server.SseClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseClientWrapperTestCase {

  @Mock
  private SseClient mockClient;

  private SseClientWrapper clientWrapper;

  @BeforeEach
  void setUp() {
    clientWrapper = new SseClientWrapper(mockClient);
  }

  @Test
  void sendEvent() throws IOException {
    clientWrapper.sendEvent("data");
    verify(mockClient).sendEvent("data");
    reset(mockClient);

    clientWrapper.sendEvent("name", "data");
    verify(mockClient).sendEvent("name", "data");
    reset(mockClient);

    clientWrapper.sendEvent("name", "data", "id");
    verify(mockClient).sendEvent("name", "data", "id");
    reset(mockClient);

    clientWrapper.sendEvent("name", "data", "id", 123L);
    verify(mockClient).sendEvent("name", "data", "id", 123L);
  }

  @Test
  void sendComment() {
    clientWrapper.sendComment("comment");
    verify(mockClient).sendComment("comment");
  }

  @Test
  void onClose() {
    Consumer<Throwable> callback = mock(Consumer.class);
    clientWrapper.onClose(callback);
    verify(mockClient).onClose(callback);
  }

  @Test
  void getClientId() {
    when(mockClient.getClientId()).thenReturn("id");
    assertThat(clientWrapper.getClientId(), is("id"));
  }

  @Test
  void getRemoteAddress() {
    InetSocketAddress remoteAddress = createUnresolved("localhost", 1234);
    when(mockClient.getRemoteAddress()).thenReturn(remoteAddress);
    assertThat(clientWrapper.getRemoteAddress(), is(remoteAddress));
  }

  @Test
  void close() throws IOException {
    clientWrapper.close();
    verify(mockClient).close();
  }
}
