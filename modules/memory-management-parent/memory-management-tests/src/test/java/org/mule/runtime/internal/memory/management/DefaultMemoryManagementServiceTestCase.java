/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import static org.mule.runtime.api.memory.provider.type.ByteBufferType.DIRECT;
import static org.mule.runtime.api.memory.provider.type.ByteBufferType.HEAP;
import static org.mule.runtime.internal.memory.management.DefaultMemoryManagementService.getInstance;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MEMORY_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MemoryManagementServiceStory.DEFAULT_MEMORY_MANAGEMENT_SERVICE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;

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
  public void correctByteBufferProviderObtainedHeap() throws Exception {
    ByteBufferProvider<ByteBuffer> byteBufferProvider =
        memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, HEAP);
    ByteBuffer byteBuffer = byteBufferProvider.allocate(100);
    assertThat(byteBuffer.isDirect(), Matchers.equalTo(FALSE));
  }

  @Test
  @Description("When a byte buffer provider is requested with a DIRECT type,  direct buffers are retrieved")
  public void correctByteBufferProviderObtainedDirect() throws Exception {
    ByteBufferProvider<ByteBuffer> byteBufferProvider =
        memoryManagementService.getByteBufferProvider(TEST_BYTE_BUFFER, DIRECT);
    ByteBuffer byteBuffer = byteBufferProvider.allocate(100);
    assertThat(byteBuffer.isDirect(), Matchers.equalTo(TRUE));
  }

  @Test
  @Description("When a bytebuffer provider with a null name is returned, an exception is raised.")
  public void byteBufferProvidersWithNullName() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Byte buffer provider name cannot be null.");
    memoryManagementService.getByteBufferProvider(null, HEAP);
  }

  @Test
  @Description("When a bytebuffer provider with a null name is returned, an exception is raised.")
  public void byteBufferProvidersWithNullNameAndPoolingConfiguration() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Byte buffer provider name cannot be null.");
    memoryManagementService.getByteBufferProvider(null, HEAP, mock(ByteBufferPoolConfiguration.class));
  }

  @Test
  @Description("When disposing a not present bytebuffer provider, no exceptions should be raised.")
  public void disposingByteBufferProviderNotPresent() {
    String notPresentByteBuffer = "notPresentByteBuffer";
    memoryManagementService.disposeByteBufferProvider(notPresentByteBuffer);
    memoryManagementService.disposeByteBufferProvider(notPresentByteBuffer);
  }

  @Test
  @Description("When the provider count usage is 0, the bytebuffer provider is disposed.")
  public void testDisposeByteBufferProviderDisposesWhenCountIsZero() {
    Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders = new HashMap<>();
    Map<String, Long> byteBufferProvidersUsageCount = new HashMap<>();
    memoryManagementService = new DefaultMemoryManagementService(byteBufferProviders, byteBufferProvidersUsageCount);

    String providerName = "testProvider";

    ByteBufferProvider<ByteBuffer> mockProvider = mock(ByteBufferProvider.class);
    byteBufferProviders.put(providerName, mockProvider);
    byteBufferProvidersUsageCount.put(providerName, 1L);

    memoryManagementService.disposeByteBufferProvider(providerName);

    // Verify that the provider is disposed and removed when the count reaches zero
    verify(mockProvider, times(1)).dispose();
    assertThat(byteBufferProviders, not(hasKey(providerName)));
    assertThat(byteBufferProvidersUsageCount, not(hasKey(providerName)));
  }

  @Test
  @Description("When the provider count usage is not 0, the bytebuffer provider is not disposed.")
  public void testDisposeByteBufferProviderDoesNotDisposeWhenCountIsGreaterThanZero() {
    Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders = new HashMap<>();
    Map<String, Long> byteBufferProvidersUsageCount = new HashMap<>();
    memoryManagementService = new DefaultMemoryManagementService(byteBufferProviders, byteBufferProvidersUsageCount);

    String providerName = "testProvider";

    ByteBufferProvider<ByteBuffer> mockProvider = mock(ByteBufferProvider.class);
    byteBufferProviders.put(providerName, mockProvider);
    byteBufferProvidersUsageCount.put(providerName, 2L);

    memoryManagementService.disposeByteBufferProvider(providerName);

    // Verify that the provider is not disposed and count is decremented
    verify(mockProvider, never()).dispose();
    assertThat(byteBufferProviders, hasKey(providerName));
    assertThat(byteBufferProvidersUsageCount.get(providerName), equalTo(1L));
  }


  @Test
  @Description("When provider not present, a warning is logged")
  public void testDisposeByteBufferProviderLogsWarningWhenProviderNotPresent() {
    String providerName = "nonExistentProvider";

    Logger mockLogger = mock(Logger.class);
    DefaultMemoryManagementService.LOGGER = mockLogger; // Inject mock logger

    memoryManagementService.disposeByteBufferProvider(providerName);

    // Verify that a warning is logged when the provider is not present
    verify(mockLogger, times(1)).warn("Unable to dispose not present ByteBufferProvider '{}'", providerName);
  }

  @Test
  @Description("Tests that byte buffer provider is only disposed when there is no more components using it.")
  public void testGetAndDisposeByteBufferProvider() {
    Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders = new HashMap<>();
    Map<String, Long> byteBufferProvidersUsageCount = new HashMap<>();

    memoryManagementService = new DefaultMemoryManagementService(byteBufferProviders, byteBufferProvidersUsageCount);

    String providerName = "testProvider";

    // Get the provider twice
    memoryManagementService.getByteBufferProvider(providerName, HEAP);
    memoryManagementService.getByteBufferProvider(providerName, HEAP);

    // Verify that the provider is in the map and usage count is 2
    assertThat(byteBufferProviders, hasKey(providerName));
    assertThat(byteBufferProvidersUsageCount.get(providerName), equalTo(2L));

    // Dispose the provider once
    memoryManagementService.disposeByteBufferProvider(providerName);

    // Verify that the count is decremented
    assertThat(byteBufferProviders, hasKey(providerName));
    assertThat(byteBufferProvidersUsageCount.get(providerName), equalTo(1L));

    // Dispose the provider again
    memoryManagementService.disposeByteBufferProvider(providerName);

    // Verify that the provider is removed from the map
    assertThat(byteBufferProviders, not(hasKey(providerName)));
    assertThat(byteBufferProvidersUsageCount, not(hasKey(providerName)));
  }
}
