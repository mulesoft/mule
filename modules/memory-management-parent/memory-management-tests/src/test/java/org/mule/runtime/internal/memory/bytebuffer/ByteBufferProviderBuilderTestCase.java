/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE;
import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.PROFILING_SERVICE_CANNOT_BE_NULL_MESSAGE;
import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.buildByteBufferProviderFrom;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MEMORY_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MemoryManagementServiceStory.BYTE_BUFFER_PROVIDER;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(MEMORY_MANAGEMENT)
@Story(BYTE_BUFFER_PROVIDER)
public class ByteBufferProviderBuilderTestCase {

  public static final String TEST_BYTE_BUFFER_PROVIDER_NAME = "test-byte-buffer-provider";
  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void whenBuilderConfiguredWithNullProfilingServiceAnExceptionIsRaised() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(PROFILING_SERVICE_CANNOT_BE_NULL_MESSAGE);
    buildByteBufferProviderFrom(ByteBufferType.HEAP)
        .withProfilingService(null)
        .build();
  }

  @Test
  public void whenBuilderConfiguredWithNullNameAnExceptionIsRaised() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE);
    buildByteBufferProviderFrom(ByteBufferType.HEAP)
        .withName(null)
        .build();
  }

  @Test
  public void buildSuccess() {
    ByteBufferProvider byteBufferProvider = buildByteBufferProviderFrom(ByteBufferType.HEAP)
        .withName(TEST_BYTE_BUFFER_PROVIDER_NAME)
        .build();

    assertThat(byteBufferProvider, is(notNullValue()));
  }
}
