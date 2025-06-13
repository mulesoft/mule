/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.muletosdk;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.HttpProtocolVersion;

import java.net.URI;
import java.util.Arrays;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpRequestWrapperTestCase {

  @Mock
  private org.mule.runtime.http.api.domain.message.request.HttpRequest muleRequest;

  private HttpRequestWrapper requestWrapper;

  @BeforeEach
  void setUp() {
    requestWrapper = new HttpRequestWrapper(muleRequest);
  }

  @Test
  void getProtocolVersion() {
    when(muleRequest.getProtocol()).thenReturn(HttpProtocol.HTTP_0_9);
    assertThat(requestWrapper.getProtocolVersion(), is(HttpProtocolVersion.HTTP_0_9));
  }

  @Test
  void getPath() {
    when(muleRequest.getPath()).thenReturn("/path");
    assertThat(requestWrapper.getPath(), is("/path"));
  }

  @Test
  void getMethod() {
    when(muleRequest.getMethod()).thenReturn("GET");
    assertThat(requestWrapper.getMethod(), is("GET"));
  }

  @Test
  void getUri() {
    when(muleRequest.getUri()).thenReturn(URI.create("/path"));
    assertThat(requestWrapper.getUri().getPath(), is("/path"));
  }

  @Test
  void getQueryParams() {
    MultiMap<String, String> queryParams = new MultiMap<>();
    queryParams.put("param1", "value1");
    queryParams.put("param2", "value2");
    when(muleRequest.getQueryParams()).thenReturn(queryParams);
    assertThat(requestWrapper.getQueryParams(), allOf(hasEntry("param1", "value1"), hasEntry("param2", "value2")));
  }

  @Test
  void getEntity() {
    HttpEntity entity = new EmptyHttpEntity();
    when(muleRequest.getEntity()).thenReturn(entity);
    assertThat(requestWrapper.getEntity().getBytesLength().isPresent(), is(true));
    assertThat(requestWrapper.getEntity().getBytesLength().getAsLong(), is(0L));
  }

  @Test
  void getHeaderNames() {
    when(muleRequest.getHeaderNames()).thenReturn(Arrays.asList("header1", "header2"));
    assertThat(requestWrapper.getHeaderNames(), contains("header1", "header2"));
  }

  @Test
  void getHeaderValue() {
    when(muleRequest.getHeaderValue("header1")).thenReturn("value1");
    assertThat(requestWrapper.getHeaderValue("header1"), is("value1"));
  }

  @Test
  void getHeaderValues() {
    when(muleRequest.getHeaderValues("header1")).thenReturn(Arrays.asList("value1", "value2"));
    assertThat(requestWrapper.getHeaderValues("header1"), contains("value1", "value2"));
  }

  @Test
  void containsHeader() {
    when(muleRequest.containsHeader("header1")).thenReturn(true);
    assertThat(requestWrapper.containsHeader("header1"), is(true));
  }

  @Test
  void getHeaders() {
    MultiMap<String, String> headers = new MultiMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    when(muleRequest.getHeaders()).thenReturn(headers);
    assertThat(requestWrapper.getHeaders(), allOf(hasEntry("header1", "value1"), hasEntry("header2", "value2")));
  }
}
