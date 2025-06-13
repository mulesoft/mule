/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.util.MultiMap;
import org.mule.sdk.api.http.domain.entity.HttpEntity;

import java.io.IOException;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Feature(HTTP_FORWARD_COMPATIBILITY)
class HttpResponseBuilderWrapperTestCase {

  private HttpResponseBuilderWrapper builderWrapper;

  @BeforeEach
  void setUp() {
    builderWrapper = new HttpResponseBuilderWrapper();
  }

  @Test
  void statusCode() {
    builderWrapper.statusCode(207);
    assertThat(builderWrapper.getStatusCode(), is(207));
    var response = builderWrapper.build();
    assertThat(response.getStatusCode(), is(207));
  }

  @Test
  void reasonPhrase() {
    builderWrapper.reasonPhrase("Hello");
    assertThat(builderWrapper.getReasonPhrase(), is("Hello"));
    var response = builderWrapper.build();
    assertThat(response.getReasonPhrase(), is("Hello"));
  }

  @Test
  void entity() throws IOException {
    HttpEntity entity = new HttpEntityFactoryImpl().fromString("test content", UTF_8);
    builderWrapper.entity(entity);
    var response = builderWrapper.build();
    assertThat(response.getEntity().getBytes(), is("test content".getBytes()));
  }

  @Test
  void addHeader() {
    builderWrapper.addHeader("key", "value");
    assertThat(builderWrapper.getHeaderValue("key").isPresent(), is(true));
    assertThat(builderWrapper.getHeaderValue("key").get(), is("value"));

    var response = builderWrapper.build();
    assertThat(response.getHeaderValue("key"), is("value"));
  }

  @Test
  void addHeaders() {
    builderWrapper.addHeaders("header", asList("value1", "value2"));
    assertThat(builderWrapper.getHeaderValues("header"), hasItems("value1", "value2"));

    var response = builderWrapper.build();
    assertThat(response.getHeaderValues("header"), hasItems("value1", "value2"));
  }

  @Test
  void removeHeader() {
    builderWrapper
        .addHeader("test-header", "test-value")
        .removeHeader("test-header");

    var response = builderWrapper.build();
    assertThat(response.containsHeader("test-header"), is(false));
  }

  @Test
  void headers() {
    var headers = new MultiMap<String, String>();
    headers.put("key1", "value1");
    builderWrapper.headers(headers);
    assertThat(builderWrapper.getHeaders().getAll("key1"), hasItem("value1"));

    var response = builderWrapper.build();
    assertThat(response.getHeaders().getAll("key1"), hasItem("value1"));
  }
}
