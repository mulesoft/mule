/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.sse.client.SseListener;
import org.mule.sdk.api.http.sse.client.SseSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpClientWrapperTestCase {

  @Mock
  private org.mule.runtime.http.api.client.HttpClient delegateClient;

  @Mock
  private HttpRequest sdkRequest;

  @Mock
  private HttpResponse muleResponse;

  @Mock
  private org.mule.runtime.http.api.sse.client.SseSource muleSseSource;

  private HttpClientWrapper clientWrapper;

  @BeforeEach
  void setUp() {
    clientWrapper = new HttpClientWrapper(delegateClient);
  }

  @Test
  void sendAsyncForwardsRequestAndOptions() throws Exception {
    var delegateFuture = completedFuture(muleResponse);
    when(delegateClient.sendAsync(any(), any(HttpRequestOptions.class))).thenReturn(delegateFuture);
    int customResponseTimeout = 1234;
    when(sdkRequest.getMethod()).thenReturn("HEAD");

    var result = clientWrapper.sendAsync(sdkRequest, config -> config.setResponseTimeout(customResponseTimeout));

    var muleRequestCaptor = forClass(org.mule.runtime.http.api.domain.message.request.HttpRequest.class);
    var muleOptionsCaptor = forClass(HttpRequestOptions.class);

    // Ensure that the future is completed, since the delegate future was
    assertThat(result.get(), notNullValue());

    verify(delegateClient).sendAsync(muleRequestCaptor.capture(), muleOptionsCaptor.capture());
    assertThat(muleOptionsCaptor.getValue().getResponseTimeout(), is(customResponseTimeout));
    assertThat(muleRequestCaptor.getValue().getMethod(), is("HEAD"));
  }

  @Test
  void sendAsyncWithException() {
    var expectedException = new RuntimeException("Test error");
    CompletableFuture<HttpResponse> delegateFuture = failedFuture(expectedException);
    when(delegateClient.sendAsync(any(), any(HttpRequestOptions.class))).thenReturn(delegateFuture);

    var result = clientWrapper.sendAsync(sdkRequest, config -> {
    });

    var gotException = assertThrows(ExecutionException.class, result::get);
    assertThat(gotException.getCause(), is(expectedException));
  }

  @Test
  void getSseSource() {
    SseListener sdkListener = mock(SseListener.class);

    when(delegateClient.sseSource(any())).thenReturn(muleSseSource);

    SseSource sseSource = clientWrapper.sseSource(config -> config.withUrl("/sse"));
    assertThat(sseSource, notNullValue());
    sseSource.register(sdkListener);

    verify(muleSseSource).register(any());
  }

  @Test
  void delegateStart() {
    clientWrapper.start();
    verify(delegateClient).start();
  }

  @Test
  void delegateStop() {
    clientWrapper.stop();
    verify(delegateClient).stop();
  }
}
