/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.sdktomule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpPartWrapperTestCase {

  @Mock
  private Part sdkPart;

  private HttpPartWrapper partWrapper;

  @BeforeEach
  void setUp() throws IOException {
    when(sdkPart.getName()).thenReturn("test-part");
    when(sdkPart.getFileName()).thenReturn("test.txt");
    when(sdkPart.getContentType()).thenReturn("text/plain");
    when(sdkPart.getSize()).thenReturn(12L);
    when(sdkPart.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    partWrapper = new HttpPartWrapper(sdkPart);
  }

  @Test
  void justConstructor() {
    assertThat(partWrapper.getName(), is("test-part"));
    assertThat(partWrapper.getFileName(), is("test.txt"));
    assertThat(partWrapper.getContentType(), is("text/plain"));
    assertThat(partWrapper.getSize(), is(12L));
  }

  @Test
  void getInputStream() throws IOException {
    InputStream result = partWrapper.getInputStream();
    assertThat(new String(result.readAllBytes()), is("test content"));
  }

  @Test
  void inputStreamWithIOException() throws IOException {
    when(sdkPart.getInputStream()).thenThrow(new IOException("Test exception"));
    HttpPartWrapper errorWrapper = new HttpPartWrapper(sdkPart);
    InputStream result = errorWrapper.getInputStream();
    assertThat(result.readAllBytes(), is(new byte[0]));
  }
}
