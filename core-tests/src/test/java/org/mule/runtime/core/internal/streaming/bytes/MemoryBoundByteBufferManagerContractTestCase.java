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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.MAX_STREAMING_MEMORY_PERCENTAGE;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.streaming.bytes.ManagedByteBufferWrapper;
import org.mule.runtime.core.internal.streaming.MemoryManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class MemoryBoundByteBufferManagerContractTestCase extends AbstractMuleTestCase {

  private static final int CAPACITY = 100;
  private static final int OTHER_CAPACITY = CAPACITY + 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MemoryBoundByteBufferManager bufferManager = createDefaultBoundBuffer();

  @After
  public void dispose() {
    if (bufferManager instanceof Disposable) {
      ((Disposable) bufferManager).dispose();
    }
  }

  @Test
  public void capacity() {
    assertCapacity(CAPACITY);
    assertCapacity(OTHER_CAPACITY);
  }

  protected abstract MemoryBoundByteBufferManager createDefaultBoundBuffer();

  protected abstract MemoryBoundByteBufferManager createBuffer(MemoryManager memoryManager, int capacity);

  @Test
  public void limitTotalMemory() {
    long maxMemory = round((DEFAULT_BUFFER_BUCKET_SIZE * 2) / MAX_STREAMING_MEMORY_PERCENTAGE);

    dispose();
    bufferManager = createBuffer(getMemoryManager(maxMemory), DEFAULT_BUFFER_BUCKET_SIZE);

    assertMemoryLimit(DEFAULT_BUFFER_BUCKET_SIZE);
  }

  @Test
  public void limitTotalMemoryThroughSystemProperty() {
    long maxMemory = round((DEFAULT_BUFFER_BUCKET_SIZE * 2) / MAX_STREAMING_MEMORY_PERCENTAGE);
    MemoryManager memoryManager = getMemoryManager(maxMemory);

    dispose();
    setProperty(MULE_STREAMING_MAX_MEMORY, String.valueOf(maxMemory));
    try {
      bufferManager = createBuffer(memoryManager, DEFAULT_BUFFER_BUCKET_SIZE);
      assertMemoryLimit(DEFAULT_BUFFER_BUCKET_SIZE);
      verify(memoryManager, never()).getMaxMemory();
    } finally {
      clearProperty(MULE_STREAMING_MAX_MEMORY);
    }
  }

  @Test
  public void invalidMemoryCapThroughSystemProperty() {
    setProperty(MULE_STREAMING_MAX_MEMORY, "don't spend that much memory please");
    dispose();
    try {
      expectedException.expect(IllegalArgumentException.class);
      bufferManager = createBuffer(mock(MemoryManager.class), DEFAULT_BUFFER_BUCKET_SIZE);
    } finally {
      clearProperty(MULE_STREAMING_MAX_MEMORY);
    }
  }

  private void assertMemoryLimit(int bufferCapacity) {
    ManagedByteBufferWrapper buffer1 = bufferManager.allocateManaged(bufferCapacity);
    ManagedByteBufferWrapper buffer2 = bufferManager.allocateManaged(bufferCapacity);
    assertThat(buffer1.getDelegate().capacity(), is(bufferCapacity));
    assertThat(buffer2.getDelegate().capacity(), is(bufferCapacity));

    try {
      bufferManager.allocateManaged(bufferCapacity);
      fail("MaxStreamingMemoryExceededException was expected");
    } catch (MaxStreamingMemoryExceededException e) {
      // awesome... continue
    }

    buffer1.release();
    buffer1 = bufferManager.allocateManaged(bufferCapacity);
    assertThat(buffer1.getDelegate().capacity(), is(bufferCapacity));
  }

  private void assertCapacity(int capacity) {
    ManagedByteBufferWrapper buffer = bufferManager.allocateManaged(capacity);
    try {
      assertThat(buffer.getDelegate().capacity(), is(capacity));
    } finally {
      buffer.release();
    }
  }

  private MemoryManager getMemoryManager(long maxMemory) {
    MemoryManager memoryManager = mock(MemoryManager.class);
    when(memoryManager.getMaxMemory()).thenReturn(maxMemory);

    return memoryManager;
  }
}
