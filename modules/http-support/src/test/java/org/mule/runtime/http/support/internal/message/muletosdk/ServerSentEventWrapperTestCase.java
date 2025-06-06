/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.muletosdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerSentEventWrapperTestCase {

  @Mock
  private org.mule.runtime.http.api.sse.ServerSentEvent muleEvent;

  private ServerSentEventWrapper sseWrapper;

  @BeforeEach
  void setUp() {
    sseWrapper = new ServerSentEventWrapper(muleEvent);
  }

  @Test
  void getName() {
    String name = "test-event";
    when(muleEvent.getName()).thenReturn(name);
    assertThat(sseWrapper.getName(), is(name));
  }

  @Test
  void getData() {
    String data = "test data";
    when(muleEvent.getData()).thenReturn(data);
    assertThat(sseWrapper.getData(), is(data));
  }

  @Test
  void getId() {
    String id = "123";
    when(muleEvent.getId()).thenReturn(Optional.of(id));
    Optional<String> result = sseWrapper.getId();
    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(id));
  }

  @Test
  void getIdEmpty() {
    when(muleEvent.getId()).thenReturn(Optional.empty());
    Optional<String> result = sseWrapper.getId();
    assertThat(result.isPresent(), is(false));
  }

  @Test
  void getRetryDelay() {
    Long retryDelay = 5000L;
    when(muleEvent.getRetryDelay()).thenReturn(Optional.of(retryDelay));
    Optional<Long> result = sseWrapper.getRetryDelay();
    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(retryDelay));
  }

  @Test
  void getRetryDelayEmpty() {
    when(muleEvent.getRetryDelay()).thenReturn(Optional.empty());
    Optional<Long> result = sseWrapper.getRetryDelay();
    assertThat(result.isPresent(), is(false));
  }
}
