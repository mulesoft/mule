/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.api;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClientFactory;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.sdk.api.http.client.ClientCreationException;
import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.response.HttpResponseBuilder;
import org.mule.sdk.api.http.server.HttpServer;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpServiceApiDelegateTestCase {

  @Mock
  private HttpService httpService;

  @Mock
  private HttpClientFactory httpClientFactory;

  @Mock
  private HttpServerFactory httpServerFactory;

  private HttpServiceApiDelegate delegate;

  @BeforeEach
  void setUp() {
    delegate = new HttpServiceApiDelegate();
    delegate.setHttpService(of(httpService));
  }

  @Test
  void clientCreation() throws ClientCreationException {
    when(httpService.getClientFactory()).thenReturn(httpClientFactory);
    when(httpClientFactory.create(any())).thenReturn(mock(org.mule.runtime.http.api.client.HttpClient.class));

    HttpClient client = delegate.client(config -> config.setName("test-client"));

    assertThat(client, notNullValue());
    verify(httpService).getClientFactory();
    verify(httpClientFactory).create(any());
  }

  @Test
  void serverCreation() throws Exception {
    when(httpService.getServerFactory()).thenReturn(httpServerFactory);
    when(httpServerFactory.create(any())).thenReturn(mock(org.mule.runtime.http.api.server.HttpServer.class));

    HttpServer server = delegate.server(config -> {
      config.setName("test-server");
      config.setHost("localhost");
    });

    assertThat(server, notNullValue());
    verify(httpService).getServerFactory();
    verify(httpServerFactory).create(any());
  }

  @Test
  void serverCreationFailure() throws Exception {
    when(httpService.getServerFactory()).thenReturn(httpServerFactory);
    when(httpServerFactory.create(any())).thenThrow(new ServerCreationException("Test error"));

    var exception = assertThrows(org.mule.sdk.api.http.server.ServerCreationException.class, () -> {
      delegate.server(config -> {
        config.setName("test-server");
        config.setHost("localhost");
      });
    });

    assertThat(exception.getMessage(), containsString("Test error"));
  }

  @Test
  void getResponseBuilder() {
    assertThat(delegate.responseBuilder(), notNullValue());
  }

  @Test
  void getResponseBuilderWithOriginal() {
    HttpResponse original = mock(HttpResponse.class);
    when(original.getStatusCode()).thenReturn(200);
    when(original.getReasonPhrase()).thenReturn("OK");

    HttpResponseBuilder builder = delegate.responseBuilder(original);

    assertThat(builder, notNullValue());
    assertThat(builder.getStatusCode(), is(200));
    assertThat(builder.getReasonPhrase(), is("OK"));
  }

  @Test
  void getRequestBuilder() {
    assertThat(delegate.requestBuilder(), notNullValue());
  }

  @Test
  void requestBuilderWithPreserveHeaderCase() {
    HttpRequest req = delegate.requestBuilder(true)
        .uri("https://example.com")
        .addHeader("CaseSensitive", "HelloWorld")
        .build();
    assertThat(req.getHeaderNames(), contains(is("CaseSensitive")));
  }

  @Test
  void getEntityFactory() {
    assertThat(delegate.entityFactory(), notNullValue());
  }
}
