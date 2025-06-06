/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.sdktomule;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.sdk.api.http.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpEntityWrapperTestCase {

  @Mock
  private HttpEntity sdkEntity;

  private HttpEntityWrapper entityWrapper;

  @BeforeEach
  void setUp() {
    entityWrapper = new HttpEntityWrapper(sdkEntity);
  }

  @Test
  void isStreaming() {
    when(sdkEntity.isStreaming()).thenReturn(true);
    assertThat(entityWrapper.isStreaming(), is(true));
  }

  @Test
  void isComposed() {
    when(sdkEntity.isComposed()).thenReturn(true);
    assertThat(entityWrapper.isComposed(), is(true));
  }

  @Test
  void getContent() {
    var inputStream = new ByteArrayInputStream("hello".getBytes());
    when(sdkEntity.getContent()).thenReturn(inputStream);
    assertThat(entityWrapper.getContent(), is(inputStream));
  }

  @Test
  void getBytes() throws IOException {
    var bytes = "hello".getBytes();
    when(sdkEntity.getBytes()).thenReturn(bytes);
    assertThat(entityWrapper.getBytes(), is(bytes));
  }

  @Test
  void getParts() throws IOException {
    Part part1 = mockPart();
    Part part2 = mockPart();
    when(sdkEntity.getParts()).thenReturn(List.of(part1, part2));
    assertThat(entityWrapper.getParts(), hasSize(2));
  }

  private static Part mockPart() throws IOException {
    Part part = mock(Part.class);
    when(part.getInputStream()).thenReturn(new ByteArrayInputStream("part".getBytes()));
    return part;
  }

  @Test
  void getLength() {
    when(sdkEntity.getBytesLength()).thenReturn(OptionalLong.of(12L));
    Optional<Long> gotLength = entityWrapper.getLength();
    assertThat(gotLength.isPresent(), is(true));
    assertThat(gotLength.get(), is(12L));

    when(sdkEntity.getBytesLength()).thenReturn(OptionalLong.empty());
    assertThat(entityWrapper.getLength(), is(Optional.empty()));
  }

  @Test
  void getBytesLength() {
    when(sdkEntity.getBytesLength()).thenReturn(OptionalLong.of(12L));
    OptionalLong gotLength = entityWrapper.getBytesLength();
    assertThat(gotLength.isPresent(), is(true));
    assertThat(gotLength.getAsLong(), is(12L));

    when(sdkEntity.getBytesLength()).thenReturn(OptionalLong.empty());
    assertThat(entityWrapper.getLength(), is(Optional.empty()));
  }
}
