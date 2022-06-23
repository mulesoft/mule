/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import static org.mule.runtime.api.memory.provider.type.ByteBufferType.DIRECT;
import static org.mule.runtime.api.memory.provider.type.ByteBufferType.HEAP;
import static org.mule.runtime.internal.memory.management.DefaultMemoryManagementService.DUPLICATE_BYTE_BUFFER_PROVIDER_NAME;
import static org.mule.runtime.internal.memory.management.DefaultMemoryManagementService.getInstance;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MEMORY_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MemoryManagementServiceStory.DEFAULT_MEMORY_MANAGEMENT_SERVICE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.junit.MockitoJUnit.rule;

import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.ByteBuffer;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoRule;

@Feature(MEMORY_MANAGEMENT)
@Story(DEFAULT_MEMORY_MANAGEMENT_SERVICE)
public class DefaultMemoryManagementServiceTestCase extends AbstractMuleTestCase {

  public static final String TEST_BYTE_BUFFER = "testByteBuffer";

  DefaultMemoryManagementService memoryManagementService = getInstance();

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public MockitoRule rule = rule();

  @After
  public void after() {
    memoryManagementService.dispose();
  }

  @Test
  @Description("When a byte buffer provider is requested with a HEAP type, no direct buffers are retrieved")
  public void correctByteBufferProviderObtainedHeap() {
    ByteBufferProvider<ByteBuffer> byteBufferProvider =
        memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, HEAP);
    ByteBuffer byteBuffer = byteBufferProvider.allocate(100);
    assertThat(byteBuffer.isDirect(), Matchers.equalTo(FALSE));
  }

  @Test
  @Description("When a byte buffer provider is requested with a DIRECT type,  direct buffers are retrieved")
  public void correctByteBufferProviderObtainedDirect() {
    ByteBufferProvider<ByteBuffer> byteBufferProvider =
        memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, DIRECT);
    ByteBuffer byteBuffer = byteBufferProvider.allocate(100);
    assertThat(byteBuffer.isDirect(), Matchers.equalTo(TRUE));
  }

  @Test
  @Description("When a bytebuffer provider with the same name is returned, an exception is raised.")
  public void byteBufferProvidersWithTheSameName() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(DUPLICATE_BYTE_BUFFER_PROVIDER_NAME, TEST_BYTE_BUFFER));
    memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, HEAP);
    memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, DIRECT);
  }

  @Test
  @Description("When disposing a not present bytebuffer provider, no exceptions should be raised.")
  public void disposingByteBufferProviderNotPresent() {
    String notPresentByteBuffer = "notPresentByteBuffer";
    memoryManagementService.disposeByteBufferProvider(notPresentByteBuffer);
    memoryManagementService.disposeByteBufferProvider(notPresentByteBuffer);
  }
}
