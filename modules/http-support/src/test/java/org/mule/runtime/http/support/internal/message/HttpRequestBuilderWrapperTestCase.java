/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.api.util.MultiMap;
import org.mule.sdk.api.http.HttpConstants;
import org.mule.sdk.api.http.domain.HttpProtocolVersion;
import org.mule.sdk.api.http.domain.entity.HttpEntity;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpRequestBuilderWrapperTestCase {

  private static final String TEST_URI = "https://example.com";

  private HttpRequestBuilderWrapper builderWrapper;

  @BeforeEach
  void setUp() {
    builderWrapper = new HttpRequestBuilderWrapper();
  }

  @Test
  void uriWithString() {
    builderWrapper.uri(TEST_URI);
    assertThat(builderWrapper.getUri().toString(), is(TEST_URI));

    var request = builderWrapper.build();
    assertThat(request.getUri().toString(), is(TEST_URI));
  }

  @Test
  void uriWithURI() {
    builderWrapper.uri(URI.create(TEST_URI));
    assertThat(builderWrapper.getUri().toString(), is(TEST_URI));

    var request = builderWrapper.build();
    assertThat(request.getUri().toString(), is(TEST_URI));
  }

  @Test
  void uriIsMandatory() {
    var error = assertThrows(NullPointerException.class, builderWrapper::build);
    assertThat(error.getMessage(), is("URI must be specified to create an HTTP request"));
  }

  @Test
  void protocol() {
    builderWrapper
        .uri(TEST_URI)
        .protocol(HttpProtocolVersion.HTTP_1_1);
    var request = builderWrapper.build();
    assertThat(request.getProtocolVersion(), is(HttpProtocolVersion.HTTP_1_1));
  }

  @Test
  void methodWithString() {
    String method = "POST";
    builderWrapper
        .uri(TEST_URI)
        .method(method);
    assertThat(builderWrapper.getMethod(), is(method));

    var request = builderWrapper.build();
    assertThat(request.getMethod(), is(method));
  }

  @Test
  void methodWithEnum() {
    builderWrapper
        .uri(TEST_URI)
        .method(HttpConstants.Method.HEAD);
    assertThat(builderWrapper.getMethod(), is("HEAD"));

    var request = builderWrapper.build();
    assertThat(request.getMethod(), is("HEAD"));
  }

  @Test
  void addQueryParam() {
    MultiMap<String, String> basic = new MultiMap<>();
    basic.put("key1", "value1");
    builderWrapper
        .uri(TEST_URI)
        .queryParams(basic)
        .addQueryParam("key2", "value2");
    assertThat(builderWrapper.getQueryParams(), allOf(hasEntry("key1", "value1"), hasEntry("key2", "value2")));

    var request = builderWrapper.build();
    assertThat(request.getQueryParams(), allOf(hasEntry("key1", "value1"), hasEntry("key2", "value2")));
  }

  @Test
  void entity() throws IOException {
    HttpEntity entity = new HttpEntityFactoryImpl().fromString("test content", UTF_8);
    builderWrapper
        .uri(TEST_URI)
        .entity(entity);
    var request = builderWrapper.build();
    assertThat(request.getEntity().getBytes(), is("test content".getBytes()));
  }

  @Test
  void addHeader() {
    builderWrapper
        .uri(TEST_URI)
        .addHeader("key", "value");
    assertThat(builderWrapper.getHeaderValue("key").isPresent(), is(true));
    assertThat(builderWrapper.getHeaderValue("key").get(), is("value"));

    var request = builderWrapper.build();
    assertThat(request.getHeaderValue("key"), is("value"));
  }

  @Test
  void addHeaders() {
    builderWrapper
        .uri(TEST_URI)
        .addHeaders("header", asList("value1", "value2"));
    assertThat(builderWrapper.getHeaderValues("header"), hasItems("value1", "value2"));

    var request = builderWrapper.build();
    assertThat(request.getHeaderValues("header"), hasItems("value1", "value2"));
  }

  @Test
  void removeHeader() {
    builderWrapper
        .uri(TEST_URI)
        .addHeader("test-header", "test-value")
        .removeHeader("test-header");

    var request = builderWrapper.build();
    assertThat(request.containsHeader("test-header"), is(false));
  }

  @Test
  void headers() {
    var headers = new MultiMap<String, String>();
    headers.put("key1", "value1");
    builderWrapper
        .uri(TEST_URI)
        .headers(headers);
    assertThat(builderWrapper.getHeaders().getAll("key1"), hasItem("value1"));

    var request = builderWrapper.build();
    assertThat(request.getHeaders().getAll("key1"), hasItem("value1"));
  }
}
