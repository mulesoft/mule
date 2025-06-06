/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.muletosdk;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpPartWrapperTestCase {

  @Mock
  private HttpPart mulePart;

  private HttpPartWrapper partWrapper;

  @BeforeEach
  void setUp() {
    partWrapper = new HttpPartWrapper(mulePart);
  }

  @Test
  void getInputStream() throws IOException {
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    when(mulePart.getInputStream()).thenReturn(inputStream);
    InputStream result = partWrapper.getInputStream();
    assertThat(result, is(inputStream));
  }

  @Test
  void getContentType() {
    String contentType = "text/plain";
    when(mulePart.getContentType()).thenReturn(contentType);
    String result = partWrapper.getContentType();
    assertThat(result, is(contentType));
  }

  @Test
  void getName() {
    String name = "test-part";
    when(mulePart.getName()).thenReturn(name);
    String result = partWrapper.getName();
    assertThat(result, is(name));
  }

  @Test
  void getSize() {
    long size = 123L;
    when(mulePart.getSize()).thenReturn(size);
    long result = partWrapper.getSize();
    assertThat(result, is(size));
  }

  @Test
  void getHeader() {
    String headerName = "Content-Disposition";
    String headerValue = "attachment; filename=\"test.txt\"";
    when(mulePart.getHeader(headerName)).thenReturn(headerValue);
    String result = partWrapper.getHeader(headerName);
    assertThat(result, is(headerValue));
  }

  @Test
  void getHeaders() {
    String headerName = "Multi-Value";
    Collection<String> headerValues = Arrays.asList("Val1", "Val2");
    when(mulePart.getHeaders(headerName)).thenReturn(headerValues);
    Collection<String> result = partWrapper.getHeaders(headerName);
    assertThat(result, is(headerValues));
  }

  @Test
  void getHeaderNames() {
    Collection<String> headerNames = Arrays.asList("Content-Type", "Content-Disposition");
    when(mulePart.getHeaderNames()).thenReturn(headerNames);
    Collection<String> result = partWrapper.getHeaderNames();
    assertThat(result, is(headerNames));
  }

  @Test
  void getFileName() {
    String fileName = "test.txt";
    when(mulePart.getFileName()).thenReturn(fileName);
    String result = partWrapper.getFileName();
    assertThat(result, is(fileName));
  }
}
