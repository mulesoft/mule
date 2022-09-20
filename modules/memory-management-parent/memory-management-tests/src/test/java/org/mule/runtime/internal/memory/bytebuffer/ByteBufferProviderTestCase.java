/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.internal.memory.bytebuffer.ThreadPoolBasedByteBufferProvider.DEFAULT_MAX_BUFFER_SIZE;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MEMORY_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MemoryManagementServiceStory.BYTE_BUFFER_PROVIDER;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.ByteBuffer;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

@Feature(MEMORY_MANAGEMENT)
@Story(BYTE_BUFFER_PROVIDER)
@RunWith(Parameterized.class)
public class ByteBufferProviderTestCase extends AbstractMuleTestCase {

  private static final int TEST_MAX_BUFFER_SIZE = 1000;
  private static final int TEST_BASE_BYTE_BUFFER_SIZE = 4;
  private static final int TEST_GROWTH_FACTOR = 2;
  private static final int TEST_NUMBER_OF_POOLS = 4;
  public static final String TEST_HEAP_BUFFER_PROVIDER = "test-heap-buffer-provider";
  public static final String TEST_HEAP_BUFFER_PROVIDER1 = "test-heap-buffer-provider";
  public static final String TEST_DIRECT_BUFFER_PROVIDER = "test-direct-buffer-provider";
  public static final String TEST_DIRECT_BUFFER_PROVIDER1 = "test-direct-buffer-provider";

  private final ThreadPoolBasedByteBufferProvider defaultByteBufferProvider;
  private final ThreadPoolBasedByteBufferProvider customByteBufferProvider;
  private final boolean isDirect;

  public ByteBufferProviderTestCase(ThreadPoolBasedByteBufferProvider defaultByteBufferProvider,
                                    ThreadPoolBasedByteBufferProvider customByteBufferProvider,
                                    boolean isDirect) {
    this.defaultByteBufferProvider = defaultByteBufferProvider;
    this.customByteBufferProvider = customByteBufferProvider;
    this.isDirect = isDirect;
  }

  @Parameterized.Parameters(name = "Testing Byte Buffer provider: {0}, {1}")
  public static List<Object[]> parameters() {
    ProfilingService profilingService = mock(ProfilingService.class);
    when(profilingService.getProfilingDataProducer(any(), any())).thenReturn(mock(ProfilingDataProducer.class));
    return asList(new Object[][] {
        {new HeapByteBufferProvider(TEST_HEAP_BUFFER_PROVIDER, profilingService),
            new HeapByteBufferProvider(TEST_HEAP_BUFFER_PROVIDER1, TEST_MAX_BUFFER_SIZE, TEST_BASE_BYTE_BUFFER_SIZE,
                                       TEST_GROWTH_FACTOR,
                                       TEST_NUMBER_OF_POOLS, profilingService),
            false},
        {new DirectByteBufferProvider(TEST_DIRECT_BUFFER_PROVIDER, profilingService),
            new DirectByteBufferProvider(TEST_DIRECT_BUFFER_PROVIDER1, TEST_MAX_BUFFER_SIZE, TEST_BASE_BYTE_BUFFER_SIZE,
                                         TEST_GROWTH_FACTOR,
                                         TEST_NUMBER_OF_POOLS, profilingService),
            true}
    });
  }

  @Test
  public void testDefaultStatus() {
    ByteBufferPool<ByteBuffer>[] pools = defaultByteBufferProvider.getThreadLocalPools();
    assertThat(pools.length, is(1));
    assertThat(defaultByteBufferProvider, instanceOf(getType(isDirect)));
    assertThat(pools[0].getMaxBufferSize(), is(DEFAULT_MAX_BUFFER_SIZE));
    // By default the pool is not initialized
    assertThat(pools[0].hasRemaining(), is(FALSE));
    assertThat(pools[0].remaining(), is(0));
  }

  @Test
  public void testCustomDefaultStatus() {
    ByteBufferPool<ByteBuffer>[] pools = customByteBufferProvider.getThreadLocalPools();
    assertThat(pools.length, is(5));
    int bufferSize = TEST_BASE_BYTE_BUFFER_SIZE;
    for (int i = 0; i < TEST_NUMBER_OF_POOLS; i++) {
      assertThat(pools[i].getMaxBufferSize(), is(bufferSize));
      bufferSize <<= TEST_GROWTH_FACTOR;
    }

    assertThat(pools[TEST_NUMBER_OF_POOLS].getMaxBufferSize(), is(TEST_MAX_BUFFER_SIZE));

    // By default the pool is not initialized
    assertThat(pools[0].hasRemaining(), is(FALSE));
    assertThat(pools[0].remaining(), is(0));
  }

  @Test
  public void testAllocationTest() throws Exception {
    ByteBufferPool<ByteBuffer>[] pools = customByteBufferProvider.getThreadLocalPools();

    ByteBuffer smallByteBuffer = customByteBufferProvider.allocate(1);

    // Assertion concerning the retrieved byte buffer
    assertThat(smallByteBuffer.isDirect(), is(isDirect));
    assertThat(smallByteBuffer.capacity(), is(1));
    assertThat(smallByteBuffer.limit(), is(1));
    assertThat(smallByteBuffer.position(), is(0));

    // Assertion concerning the pool. The byteBuffer should be retrieved from the first pool
    assertThat(pools[0].remaining(), is(3));
    assertThat(pools[0].hasRemaining(), is(TRUE));

    ByteBuffer mediumByteBuffer = customByteBufferProvider.allocate(12);

    assertThat(mediumByteBuffer.isDirect(), is(isDirect));
    assertThat(mediumByteBuffer.capacity(), is(12));
    assertThat(mediumByteBuffer.limit(), is(12));
    assertThat(mediumByteBuffer.position(), is(0));

    // Assertion concerning the pool. The byteBuffer should be retrieved from the first pool
    assertThat(pools[1].remaining(), is(4));
    assertThat(pools[1].hasRemaining(), is(TRUE));

    // Assert all the other pools remain intact (not initialised)
    for (int i = 2; i <= TEST_NUMBER_OF_POOLS; i++) {
      assertThat(pools[i].remaining(), is(0));
      assertThat(pools[i].hasRemaining(), is(FALSE));
    }

    customByteBufferProvider.release(mediumByteBuffer);

    assertThat(pools[1].remaining(), is(16));
    assertThat(pools[1].hasRemaining(), is(TRUE));

    customByteBufferProvider.release(smallByteBuffer);

    assertThat(pools[0].remaining(), is(4));
    assertThat(pools[0].hasRemaining(), is(TRUE));
  }

  private Class<? extends ThreadPoolBasedByteBufferProvider> getType(boolean isDirect) {
    if (isDirect) {
      return DirectByteBufferProvider.class;
    } else {
      return HeapByteBufferProvider.class;
    }
  }

}
