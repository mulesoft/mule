/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.toIntExact;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.streaming.MemoryManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.qameta.allure.Feature;

@SmallTest
@Feature(STREAMING)
public class PoolingByteBufferManagerTestCase extends AbstractMuleTestCase {

  private PoolingByteBufferManager bufferManager = new PoolingByteBufferManager();
  private static final int CAPACITY = 100;
  private static final int OTHER_CAPACITY = CAPACITY + 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void after() {
    bufferManager.dispose();
  }

  @Test
  public void pooling() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    bufferManager.deallocate(buffer);

    ByteBuffer newBuffer = bufferManager.allocate(CAPACITY);
    assertThat(buffer, is(sameInstance(newBuffer)));
  }

  @Test
  public void grow() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    ByteBuffer newBuffer = bufferManager.allocate(CAPACITY);

    assertThat(buffer, not(sameInstance(newBuffer)));
  }

  @Test
  public void differentPoolsPerCapacity() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    bufferManager.deallocate(buffer);

    ByteBuffer buffer2 = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer, not(sameInstance(buffer2)));

    ByteBuffer buffer3 = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer, not(sameInstance(buffer3)));
    assertThat(buffer2, not(sameInstance(buffer3)));

    bufferManager.deallocate(buffer2);
    ByteBuffer buffer2Reborn = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer2, is(sameInstance(buffer2Reborn)));
  }

  @Test
  public void capacity() throws Exception {
    assertCapacity(CAPACITY);
    assertCapacity(OTHER_CAPACITY);
  }

  @Test
  public void limitTotalMemory() throws Exception {
    final long maxMemory = 40L;
    final int bufferCapacity = toIntExact(maxMemory / 4);
    final long waitTimeoutMillis = SECONDS.toMillis(2);

    MemoryManager memoryManager = mock(MemoryManager.class);
    when(memoryManager.getMaxMemory()).thenReturn(maxMemory);

    bufferManager = new PoolingByteBufferManager(memoryManager, waitTimeoutMillis);

    assertMemoryLimit(bufferCapacity, waitTimeoutMillis);
  }

  @Test
  public void limitTotalMemoryThroughSystemProperty() throws Exception {
    final long maxMemory = 40;
    final int bufferCapacity = toIntExact(maxMemory / 4);
    final long waitTimeoutMillis = SECONDS.toMillis(2);

    MemoryManager memoryManager = mock(MemoryManager.class);

    setProperty(MULE_STREAMING_MAX_MEMORY, String.valueOf(maxMemory / 2));
    try {
      bufferManager = new PoolingByteBufferManager(memoryManager, waitTimeoutMillis);
      assertMemoryLimit(bufferCapacity, waitTimeoutMillis);
      verify(memoryManager, never()).getMaxMemory();
    } finally {
      clearProperty(MULE_STREAMING_MAX_MEMORY);
    }
  }

  @Test
  public void invalidMemoryCapThroughSystemProperty() throws Exception {
    setProperty(MULE_STREAMING_MAX_MEMORY, "don't spend that much memory please");
    try {
      expectedException.expect(IllegalArgumentException.class);
      bufferManager = new PoolingByteBufferManager(mock(MemoryManager.class), 10);
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
      } catch (Exception e) {
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
}
