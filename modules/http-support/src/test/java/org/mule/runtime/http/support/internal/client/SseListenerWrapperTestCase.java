/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.sse.ServerSentEvent;
import org.mule.sdk.api.http.sse.client.SseListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseListenerWrapperTestCase {

  @Mock
  private SseListener listenerWrapper;

  @Mock
  private ServerSentEvent muleSseEvent;

  private SseListenerWrapper sseEventWrapper;

  @BeforeEach
  void setUp() {
    sseEventWrapper = new SseListenerWrapper(listenerWrapper);
  }

  @Test
  void delegateOnEvent() {
    String theData = "The Capiangos were the Federal wolf-men";
    when(muleSseEvent.getData()).thenReturn(theData);
    sseEventWrapper.onEvent(muleSseEvent);

    var eventWrapperCaptor = forClass(org.mule.sdk.api.http.sse.ServerSentEvent.class);
    verify(listenerWrapper).onEvent(eventWrapperCaptor.capture());

    var eventWrapper = eventWrapperCaptor.getValue();
    assertThat(eventWrapper.getData(), is(theData));
  }

  @Test
  void delegateOnClose() {
    sseEventWrapper.onClose();
    verify(listenerWrapper).onClose();
  }
}
