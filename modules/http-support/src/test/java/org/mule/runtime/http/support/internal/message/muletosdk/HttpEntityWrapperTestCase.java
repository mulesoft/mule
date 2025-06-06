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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpEntityWrapperTestCase {

  @Mock
  private org.mule.runtime.http.api.domain.entity.HttpEntity mockMuleEntity;

  @Mock
  private org.mule.runtime.http.api.domain.entity.multipart.HttpPart mockMulePart;

  private HttpEntityWrapper wrapper;

  @BeforeEach
  void setUp() {
    wrapper = new HttpEntityWrapper(mockMuleEntity);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isStreaming(boolean streaming) {
    when(mockMuleEntity.isStreaming()).thenReturn(streaming);
    boolean result = wrapper.isStreaming();
    assertThat(result, is(streaming));
    verify(mockMuleEntity).isStreaming();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isComposed(boolean composed) {
    when(mockMuleEntity.isComposed()).thenReturn(composed);
    boolean result = wrapper.isComposed();
    assertThat(result, is(composed));
    verify(mockMuleEntity).isComposed();
  }

  @Test
  void getContentDoesNotCopyTheStream() {
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    when(mockMuleEntity.getContent()).thenReturn(inputStream);
    InputStream result = wrapper.getContent();
    assertThat(result, is(sameInstance(inputStream)));
    verify(mockMuleEntity).getContent();
  }

  @Test
  void getBytes() throws IOException {
    byte[] bytes = "test content".getBytes();
    when(mockMuleEntity.getBytes()).thenReturn(bytes);
    byte[] result = wrapper.getBytes();
    assertThat(result, is(bytes));
    verify(mockMuleEntity).getBytes();
  }

  @Test
  void getParts() throws IOException {
    when(mockMuleEntity.getParts()).thenReturn(List.of(mockMulePart));
    Collection<Part> result = wrapper.getParts();
    assertThat(result, notNullValue());
    assertThat(result.size(), is(1));
    verify(mockMuleEntity).getParts();
  }

  @ParameterizedTest
  @ValueSource(longs = {100L, 200L})
  void getBytesLength(long length) {
    OptionalLong optionalLen = OptionalLong.of(length);
    when(mockMuleEntity.getBytesLength()).thenReturn(optionalLen);

    OptionalLong result = wrapper.getBytesLength();

    assertThat(result, is(optionalLen));
    verify(mockMuleEntity).getBytesLength();
  }

  @Test
  void getBytesLengthEmpty() {
    OptionalLong optionalLen = OptionalLong.empty();
    when(mockMuleEntity.getBytesLength()).thenReturn(optionalLen);

    OptionalLong result = wrapper.getBytesLength();

    assertThat(result, is(optionalLen));
    verify(mockMuleEntity).getBytesLength();
  }
}
