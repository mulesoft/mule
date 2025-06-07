/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.sse.client.SseSourceConfig;
import org.mule.runtime.http.support.internal.message.HttpResponseBuilderWrapper;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Feature(HTTP_FORWARD_COMPATIBILITY)
class SseSourceConfigImplTestCase {

  private SseSourceConfigImpl configurer;

  @BeforeEach
  void setup() {
    configurer = new SseSourceConfigImpl();
  }

  @Test
  void mustProvideResponseOrUrl() {
    var error = assertThrows(IllegalArgumentException.class, configurer::build);
    assertThat(error.getMessage(), containsString("An HTTP Response or an URL must be provided to build an SseSource"));
  }

  @Test
  void fromResponse() {
    HttpResponse res = new HttpResponseBuilderWrapper()
        .addHeader("Content-Type", "text/event-stream")
        .build();
    configurer.withResponse(res);
    SseSourceConfig config = configurer.build();
    assertThat(config.getResponse().getHeaderValue("Content-Type"), is("text/event-stream"));
  }

  @Test
  void fromUrlOnly() {
    configurer.withUrl("/sse");
    SseSourceConfig config = configurer.build();
    assertThat(config.getUrl(), is("/sse"));
  }

  @Test
  void withRetry() {
    configurer
        .withUrl("/sse")
        .withRetry(true)
        .withAllowRetryDelayOverride(false)
        .withInitialRetryDelayMillis(1234L);
    SseSourceConfig config = configurer.build();
    var retryConfig = config.getRetryConfig();
    assertThat(retryConfig.shouldRetryOnStreamEnd(), is(true));
    assertThat(retryConfig.allowRetryDelayOverride(), is(false));
    assertThat(retryConfig.initialRetryDelayMillis(), is(1234L));
  }

  @Test
  void withRequestCustomizations() {
    configurer
        .withUrl("/sse")
        .withPreserveHeadersCase(false)
        .withRequestOptions(opts -> opts.setResponseTimeout(1234))
        .withRequestCustomizer(req -> req.addQueryParam("foo", "bar"));

    SseSourceConfig config = configurer.build();
    assertThat(config.getRequestOptions().getResponseTimeout(), is(1234));

    HttpRequestBuilder someBuilder = HttpRequest.builder().uri("/sse");
    config.getRequestCustomizer().accept(someBuilder);
    assertThat(someBuilder.build().getQueryParams().get("foo"), is("bar"));
  }
}
