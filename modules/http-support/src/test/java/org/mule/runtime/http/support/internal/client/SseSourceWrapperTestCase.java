/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.mule.runtime.http.api.sse.client.SseSource.READY_STATUS_OPEN;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.sse.client.SseSource;
import org.mule.sdk.api.http.sse.client.SseListener;

import java.util.function.Consumer;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class SseSourceWrapperTestCase {

  private SseSourceWrapper sseSourceWrapper;

  @Mock
  private SseSource muleSseSource;

  @BeforeEach
  void setUp() {
    sseSourceWrapper = new SseSourceWrapper(muleSseSource);
  }

  @Test
  void delegatesOpenToSseSource() {
    sseSourceWrapper.open();
    verify(muleSseSource).open();
  }

  @Test
  void delegatesReadyState() {
    when(muleSseSource.getReadyState()).thenReturn(READY_STATUS_OPEN);
    assertThat(sseSourceWrapper.getReadyState(), is(READY_STATUS_OPEN));
  }

  @Test
  void delegatesRegister() {
    var listener = mock(SseListener.class);
    sseSourceWrapper.register(listener);
    verify(muleSseSource).register(any(org.mule.runtime.http.api.sse.client.SseListener.class));

    sseSourceWrapper.register("topic", listener);
    verify(muleSseSource).register(eq("topic"), any(org.mule.runtime.http.api.sse.client.SseListener.class));
  }

  @Test
  void delegatesConnectionFailureCallback() {
    var callback = mock(Consumer.class);
    sseSourceWrapper.doOnConnectionFailure(callback);
    verify(muleSseSource).doOnConnectionFailure(any(Consumer.class));
  }

  @Test
  void delegatesCloseToSseSource() {
    sseSourceWrapper.close();
    verify(muleSseSource).close();
  }
}
