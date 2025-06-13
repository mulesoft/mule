/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.server.async.ResponseStatusCallback;
import org.mule.sdk.api.http.sse.server.SseClientConfig;

import java.util.function.Consumer;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpResponseReadyCallbackWrapperTestCase {

  @Mock
  private HttpResponseReadyCallback mockCallback;

  private HttpResponseReadyCallbackWrapper wrapper;

  @BeforeEach
  void setUp() {
    wrapper = new HttpResponseReadyCallbackWrapper(mockCallback);
  }

  @Test
  void responseReady() {
    HttpResponse response = mock(HttpResponse.class);
    ResponseStatusCallback statusCallback = mock(ResponseStatusCallback.class);
    wrapper.responseReady(response, statusCallback);
    verify(mockCallback).responseReady(any(), any());
  }

  @Test
  void startSseResponse() {
    Consumer<SseClientConfig> configurer = mock(Consumer.class);
    var sseClient = wrapper.startSseResponse(configurer);
    assertThat(sseClient, notNullValue());
    verify(configurer).accept(any());
  }
}
