/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.round;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.MAX_STREAMING_MEMORY_PERCENTAGE;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.streaming.MemoryManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class MemoryBoundByteBufferManagerContractTestCase extends AbstractMuleTestCase {

  private MemoryBoundByteBufferManager bufferManager = createDefaultBoundBuffer();
  private static final int CAPACITY = 100;
  private static final int OTHER_CAPACITY = CAPACITY + 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void after() {
    bufferManager.dispose();
  }

  @Test
  public void capacity() {
    assertCapacity(CAPACITY);
    assertCapacity(OTHER_CAPACITY);
  }

  protected abstract MemoryBoundByteBufferManager createDefaultBoundBuffer();

  protected abstract MemoryBoundByteBufferManager createBuffer(MemoryManager memoryManager, int capacity, long waitTimeoutMillis);

  @Test
  public void limitTotalMemory() throws Exception {
    long maxMemory = round((DEFAULT_BUFFER_BUCKET_SIZE * 2) / MAX_STREAMING_MEMORY_PERCENTAGE);
    final long waitTimeoutMillis = SECONDS.toMillis(2);

    bufferManager.dispose();
    bufferManager = createBuffer(getMemoryManager(maxMemory), DEFAULT_BUFFER_BUCKET_SIZE, waitTimeoutMillis);

    assertMemoryLimit(DEFAULT_BUFFER_BUCKET_SIZE, waitTimeoutMillis);
  }

  @Test
  public void limitTotalMemoryThroughSystemProperty() throws Exception {
    long maxMemory = round((DEFAULT_BUFFER_BUCKET_SIZE * 2) / MAX_STREAMING_MEMORY_PERCENTAGE);
    final long waitTimeoutMillis = SECONDS.toMillis(2);

    MemoryManager memoryManager = getMemoryManager(maxMemory);

    bufferManager.dispose();
    setProperty(MULE_STREAMING_MAX_MEMORY, String.valueOf(maxMemory));
    try {
      bufferManager = createBuffer(memoryManager, DEFAULT_BUFFER_BUCKET_SIZE, waitTimeoutMillis);
      assertMemoryLimit(DEFAULT_BUFFER_BUCKET_SIZE, waitTimeoutMillis);
      verify(memoryManager, never()).getMaxMemory();
    } finally {
      clearProperty(MULE_STREAMING_MAX_MEMORY);
    }
  }

  @Test
  public void invalidMemoryCapThroughSystemProperty() throws Exception {
    setProperty(MULE_STREAMING_MAX_MEMORY, "don't spend that much memory please");
    bufferManager.dispose();
    try {
      expectedException.expect(IllegalArgumentException.class);
      bufferManager = createBuffer(mock(MemoryManager.class), DEFAULT_BUFFER_BUCKET_SIZE, 10);
    } finally {
      clearProperty(MULE_STREAMING_MAX_MEMORY);
    }
  }

  private void assertMemoryLimit(int bufferCapacity, long waitTimeoutMillis) throws InterruptedException {
    ByteBuffer buffer1 = bufferManager.allocate(bufferCapacity);
    ByteBuffer buffer2 = bufferManager.allocate(bufferCapacity);
    assertThat(buffer1.capacity(), is(bufferCapacity));
    assertThat(buffer2.capacity(), is(bufferCapacity));

    Latch latch = new Latch();
    Reference<Boolean> maxMemoryExhausted = new Reference<>(false);

    new Thread(() -> {
      try {
        bufferManager.allocate(bufferCapacity);
        latch.release();
      } catch (MaxStreamingMemoryExceededException e) {
        maxMemoryExhausted.set(true);
      } catch (MuleRuntimeException e) {
        maxMemoryExhausted.set(e.getCause() instanceof MaxStreamingMemoryExceededException);
      }
    }).start();

    assertThat(latch.await(waitTimeoutMillis * 2, MILLISECONDS), is(false));
    assertThat(maxMemoryExhausted.get(), is(true));

    bufferManager.deallocate(buffer1);

    Latch secondLatch = new Latch();
    new Thread(() -> {
      try {
        bufferManager.allocate(bufferCapacity);
        maxMemoryExhausted.set(false);
      } finally {
        secondLatch.release();
      }
    }).start();

    assertThat(secondLatch.await(waitTimeoutMillis, MILLISECONDS), is(true));
    assertThat(maxMemoryExhausted.get(), is(false));
  }

  private void assertCapacity(int capacity) {
    ByteBuffer buffer = bufferManager.allocate(capacity);
    try {
      assertThat(buffer.capacity(), is(capacity));
    } finally {
      bufferManager.deallocate(buffer);
    }
  }

  private MemoryManager getMemoryManager(long maxMemory) {
    MemoryManager memoryManager = mock(MemoryManager.class);
    when(memoryManager.getMaxMemory()).thenReturn(maxMemory);

    return memoryManager;
  }
}
