/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.sdk.api.http.domain.entity.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Feature(HTTP_FORWARD_COMPATIBILITY)
class HttpEntityFactoryImplTestCase {

  private HttpEntityFactoryImpl factory;

  @BeforeEach
  void setUp() {
    factory = new HttpEntityFactoryImpl();
  }

  @Test
  void fromByteArray() throws IOException {
    byte[] content = "test content".getBytes();
    var entity = factory.fromByteArray(content);
    assertThat(entity.getBytes(), is(content));
  }

  @Test
  void fromString() throws IOException {
    String content = "test content";
    var entity = factory.fromString(content, StandardCharsets.UTF_8);
    assertThat(entity.getBytes(), is(content.getBytes()));
  }

  @Test
  void fromInputStreamWithoutContentLength() throws IOException {
    var contentBytes = "test content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(contentBytes);
    var entity = factory.fromInputStream(inputStream);
    assertThat(entity.getBytes(), is(contentBytes));
    assertThat(entity.getBytesLength().isPresent(), is(false));
  }

  @Test
  void fromInputStreamWithNullContentLength() throws IOException {
    var contentBytes = "test content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(contentBytes);
    var entity = factory.fromInputStream(inputStream, null);
    assertThat(entity.getBytes(), is(contentBytes));
    assertThat(entity.getBytesLength().isPresent(), is(false));
  }

  @Test
  void fromInputStreamWithContentLength() throws IOException {
    var contentBytes = "test content".getBytes();
    Long contentLen = (long) contentBytes.length;
    InputStream inputStream = new ByteArrayInputStream(contentBytes);
    var entity = factory.fromInputStream(inputStream, contentLen);
    assertThat(entity.getBytes(), is(contentBytes));
    assertThat(entity.getBytesLength().isPresent(), is(true));
    assertThat(entity.getBytesLength().getAsLong(), is(contentLen));
  }

  @Test
  void emptyEntity() throws IOException {
    HttpEntity entity = factory.emptyEntity();
    assertThat(entity.getBytesLength().isPresent(), is(true));
    assertThat(entity.getBytesLength().getAsLong(), is(0L));
    assertThat(entity.getBytes(), is(new byte[0]));
  }
}
