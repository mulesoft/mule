/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.sdktomule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.support.internal.message.HttpEntityFactoryImpl;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpResponseWrapperTestCase {

  @Mock
  private HttpResponse sdkResponse;

  private org.mule.runtime.http.support.internal.message.sdktomule.HttpResponseWrapper responseWrapper;

  @BeforeEach
  void setUp() {
    responseWrapper = new HttpResponseWrapper(sdkResponse);
  }

  @Test
  void getStatusCode() {
    when(sdkResponse.getStatusCode()).thenReturn(207);
    assertThat(responseWrapper.getStatusCode(), is(207));
  }

  @Test
  void getReasonPhrase() {
    when(sdkResponse.getReasonPhrase()).thenReturn("Hello");
    assertThat(responseWrapper.getReasonPhrase(), is("Hello"));
  }

  @Test
  void getEntity() {
    var entity = new HttpEntityFactoryImpl().emptyEntity();
    when(sdkResponse.getEntity()).thenReturn(entity);
    assertThat(responseWrapper.getEntity().getBytesLength().isPresent(), is(true));
    assertThat(responseWrapper.getEntity().getBytesLength().getAsLong(), is(0L));
  }

  @Test
  void getHeaderNames() {
    when(sdkResponse.getHeaderNames()).thenReturn(Arrays.asList("header1", "header2"));
    assertThat(responseWrapper.getHeaderNames(), contains("header1", "header2"));
  }

  @Test
  void containsHeader() {
    when(sdkResponse.getHeaderValue("header1")).thenReturn("Present!");
    assertThat(responseWrapper.containsHeader("header1"), is(true));
  }

  @Test
  void getHeaderValue() {
    when(sdkResponse.getHeaderValue("header1")).thenReturn("value1");
    assertThat(responseWrapper.getHeaderValue("header1"), is("value1"));
    assertThat(responseWrapper.getHeaderValueIgnoreCase("header1"), is("value1"));
  }

  @Test
  void getHeaderValues() {
    when(sdkResponse.getHeaderValues("header1")).thenReturn(Arrays.asList("value1", "value2"));
    assertThat(responseWrapper.getHeaderValues("header1"), contains("value1", "value2"));
    assertThat(responseWrapper.getHeaderValuesIgnoreCase("header1"), contains("value1", "value2"));
  }

  @Test
  void getHeaders() {
    MultiMap<String, String> headers = new MultiMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    when(sdkResponse.getHeaders()).thenReturn(headers);
    assertThat(responseWrapper.getHeaders(), allOf(hasEntry("header1", "value1"), hasEntry("header2", "value2")));
  }
}
