/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_ALLOCATION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_DEALLOCATION;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.fill;

import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;
import org.mule.runtime.internal.memory.bytebuffer.profiling.ContainerProfilingScope;
import org.mule.runtime.internal.memory.bytebuffer.profiling.DefaultByteBufferProviderEventContext;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Common logic for the different implementations for {@link ByteBufferProvider}
 *
 * @since 4.5.0
 */
public abstract class ThreadPoolBasedByteBufferProvider implements ByteBufferProvider<ByteBuffer> {

  public static final int DEFAULT_MAX_BUFFER_SIZE = 1024 * 64;
  private static final int DEFAULT_BASE_BYTE_BUFFER_SIZE = 1;
  private static final int DEFAULT_GROWTH_FACTOR = 1;
  private static final int DEFAULT_NUMBER_OF_FIX_SIZED_POOLS = 0;

  protected final int maxBufferSize;

  private final ByteBufferPool<ByteBuffer>[] pools;
  private final String name;
  ProfilingDataProducer<ByteBufferProviderEventContext, Object> allocationDataProducer;
  ProfilingDataProducer<ByteBufferProviderEventContext, Object> deallocationDataProducer;

  protected ThreadPoolBasedByteBufferProvider(String name, ProfilingService profilingService) {
    this(name, DEFAULT_MAX_BUFFER_SIZE, DEFAULT_BASE_BYTE_BUFFER_SIZE, DEFAULT_GROWTH_FACTOR, DEFAULT_NUMBER_OF_FIX_SIZED_POOLS,
         profilingService);
  }

  protected ThreadPoolBasedByteBufferProvider(String name, int maxBufferSize, int baseByteBufferSize, int growthFactor,
                                              int numberOfPools,
                                              ProfilingService profilingService) {

    this.name = name;

    if (maxBufferSize <= 0) {
      throw new IllegalArgumentException("maxBufferSize must be greater than zero");
    }

    if (baseByteBufferSize <= 0) {
      throw new IllegalArgumentException("baseByteBufferSize must be greater than zero");
    }

    if (numberOfPools < 0) {
      throw new IllegalArgumentException("baseByteBufferSize must be greater than zero");
    }

    if (!isPowerOfTwo(baseByteBufferSize) || !isPowerOfTwo(growthFactor)) {
      throw new IllegalArgumentException("minBufferSize and growthFactor must be a power of two");
    }

    this.maxBufferSize = maxBufferSize;

    pools = new ByteBufferPool[numberOfPools + 1];
    for (int i = 0, bufferSize = baseByteBufferSize; i < numberOfPools; i++, bufferSize <<= growthFactor) {
      pools[i] = new ThreadLocalByteBufferWrapper(bufferSize);
    }

    allocationDataProducer =
        profilingService.getProfilingDataProducer(MEMORY_BYTE_BUFFER_ALLOCATION, new ContainerProfilingScope());
    deallocationDataProducer =
        profilingService.getProfilingDataProducer(MEMORY_BYTE_BUFFER_DEALLOCATION, new ContainerProfilingScope());
    pools[numberOfPools] = new ThreadLocalByteBufferWrapper(maxBufferSize);
  }

  private boolean isPowerOfTwo(int valueToCheck) {
    return (valueToCheck & valueToCheck - 1) == 0;
  }

  protected abstract ByteBuffer doAllocate(int size);

  @Override
  public ByteBuffer allocate(int size) {
    allocationDataProducer
        .triggerProfilingEvent(new DefaultByteBufferProviderEventContext(name, currentTimeMillis(), size));
    return this.allocateByteBuffer(size);
  }

  private ByteBuffer allocateByteBuffer(int size) {
    if (size > maxBufferSize) {
      return doAllocate(size);
    }

    final ByteBufferPool<ByteBuffer> threadLocalCache = getByteBufferThreadLocalPool(size);
    if (threadLocalCache != null) {
      final int remaining = threadLocalCache.remaining();

      if (remaining == 0 || remaining < size) {
        ByteBuffer byteBuffer = doAllocate(threadLocalCache.getMaxBufferSize());
        threadLocalCache.reset(byteBuffer);
      }

      return (ByteBuffer) allocateFromPool(threadLocalCache, size);
    } else {
      return doAllocate(size);
    }
  }

  private Object allocateFromPool(ByteBufferPool<ByteBuffer> threadLocalCache, int size) {
    if (threadLocalCache.remaining() >= size) {
      return threadLocalCache.allocate(size);
    }

    return null;
  }

  private void reallocatePoolBuffer(int size) {
    final ByteBuffer byteBuffer = doAllocate(size);

    final ByteBufferPool<ByteBuffer> threadLocalCache = getByteBufferThreadLocalPool(size);
    if (threadLocalCache != null) {
      threadLocalCache.reset(byteBuffer);
    }
  }

  private ByteBufferPool<ByteBuffer> getByteBufferThreadLocalPool(final int size) {
    for (final ByteBufferPool<ByteBuffer> pool : pools) {
      if (pool.getMaxBufferSize() >= size) {
        return pool;
      }
    }

    return null;
  }

  @Override
  public ByteBuffer allocateAtLeast(int size) {
    return allocateByteBufferAtLeast(size);
  }

  private ByteBuffer allocateByteBufferAtLeast(int size) {
    if (size > maxBufferSize) {
      // Don't use pool
      return doAllocate(size);
    }

    final ByteBufferPool<ByteBuffer> threadLocalCache = getByteBufferThreadLocalPool(size);
    if (threadLocalCache != null) {
      int remaining = threadLocalCache.remaining();

      if (remaining == 0 || remaining < size) {
        reallocatePoolBuffer(size);
        remaining = threadLocalCache.remaining();
      }

      return (ByteBuffer) allocateFromPool(threadLocalCache, remaining);
    } else {
      return doAllocate(size);
    }
  }

  @Override
  public ByteBuffer reallocate(ByteBuffer oldBuffer, int newSize) {
    return reallocateByteBuffer(oldBuffer, newSize);
  }

  private ByteBuffer reallocateByteBuffer(ByteBuffer oldByteBuffer, int newSize) {
    if (oldByteBuffer.capacity() >= newSize) {
      return oldByteBuffer;
    }

    final ByteBufferPool<ByteBuffer> memoryPool = getByteBufferThreadLocalPool(newSize);
    if (memoryPool != null) {
      final ByteBuffer newBuffer = memoryPool.reallocate(oldByteBuffer, newSize);

      if (newBuffer != null) {
        return newBuffer;
      }
    }
    ByteBuffer newByteBuffer = allocateByteBuffer(newSize);

    if (newByteBuffer == null) {
      throw new IllegalStateException(format("It was not possible to allocate reallocate a buffer with size '%s'", newSize));
    }

    oldByteBuffer.flip();
    return newByteBuffer.put(oldByteBuffer);
  }

  @Override
  public void release(ByteBuffer byteBuffer) {
    deallocationDataProducer
        .triggerProfilingEvent(new DefaultByteBufferProviderEventContext(name, currentTimeMillis(), byteBuffer.limit()));
    ByteBufferPool<ByteBuffer> memoryPool = getByteBufferThreadLocalPool(byteBuffer.limit());
    if (memoryPool != null) {
      memoryPool.release((ByteBuffer) byteBuffer.clear());
    }
  }

  @Override
  public byte[] getByteArray(int size) {
    return new byte[size];
  }

  @Override
  public void dispose() {
    for (ByteBufferPool<ByteBuffer> pool : pools) {
      pool.dispose();
    }
  }


  protected ByteBufferPool<ByteBuffer>[] getThreadLocalPools() {
    return pools;
  }

  private static final class ThreadLocalByteBufferWrapper implements ByteBufferPool<ByteBuffer> {

    private final int bufferSize;

    private final ThreadLocal<ByteBufferPool<ByteBuffer>> delegate = new ThreadLocal<>();

    public ThreadLocalByteBufferWrapper(final int bufferSize) {
      this.bufferSize = bufferSize;
    }

    @Override
    public ByteBuffer reallocate(ByteBuffer oldByteBuffer, int newSize) {
      return getDelegate().reallocate(oldByteBuffer, newSize);
    }

    @Override
    public boolean release(ByteBuffer byteBuffer) {
      return getDelegate().release(byteBuffer);
    }

    @Override
    public ByteBuffer reduceLastAllocated(ByteBuffer byteBuffer) {
      return getDelegate().reduceLastAllocated(byteBuffer);
    }

    @Override
    public int remaining() {
      return getDelegate().remaining();
    }

    @Override
    public void reset(ByteBuffer byteBuffer) {
      getDelegate().reset(byteBuffer);
    }

    @Override
    public ByteBuffer allocate(int size) {
      return getDelegate().allocate(size);
    }

    @Override
    public boolean hasRemaining() {
      return getDelegate().hasRemaining();
    }

    @Override
    public int getMaxBufferSize() {
      return getDelegate().getMaxBufferSize();
    }

    @Override
    public void dispose() {
      delegate.remove();
    }

    private ByteBufferPool<ByteBuffer> getDelegate() {
      if (delegate.get() == null) {
        delegate.set(new ByteBufferThreadLocalPool(bufferSize));
      }

      return delegate.get();
    }
  }
  /**
   * Information about thread associated memory pool.
   */
  private static final class ByteBufferThreadLocalPool implements ByteBufferPool<ByteBuffer> {

    /**
     * Memory pool
     */
    private ByteBuffer pool;

    /**
     * {@link ByteBuffer} allocation history.
     */
    private Object[] allocationHistory;
    private int lastAllocatedIndex;
    private final int maxBufferSize;

    public ByteBufferThreadLocalPool(int maxBufferSize) {
      allocationHistory = new Object[8];
      this.maxBufferSize = maxBufferSize;
    }

    @Override
    public void reset(ByteBuffer pool) {
      fill(allocationHistory, 0, lastAllocatedIndex, null);
      lastAllocatedIndex = 0;
      this.pool = pool;
    }

    @Override
    public ByteBuffer allocate(int size) {
      final ByteBuffer allocated = slice(pool, size);
      return addHistory(allocated);
    }

    @Override
    public ByteBuffer reallocate(ByteBuffer oldByteBuffer, int newSize) {
      if (isLastAllocated(oldByteBuffer) && remaining() + oldByteBuffer.capacity() >= newSize) {

        lastAllocatedIndex--;

        pool.position(pool.position() - oldByteBuffer.capacity());
        final ByteBuffer newByteBuffer = slice(pool, newSize);
        newByteBuffer.position(oldByteBuffer.position());

        return addHistory(newByteBuffer);
      }

      return null;
    }

    private ByteBuffer slice(ByteBuffer chunk, int size) {
      chunk.limit(chunk.position() + size);
      final ByteBuffer view = chunk.slice();
      chunk.position(chunk.limit());
      chunk.limit(chunk.capacity());

      return view;
    }

    @Override
    public boolean release(ByteBuffer byteBuffer) {
      if (isLastAllocated(byteBuffer)) {
        pool.position(pool.position() - byteBuffer.capacity());
        allocationHistory[--lastAllocatedIndex] = null;

        return true;
      } else if (wantReset(byteBuffer.capacity())) {
        reset(byteBuffer);
        return true;
      }

      return false;
    }

    public boolean wantReset(int size) {
      return !hasRemaining() || lastAllocatedIndex == 0 && pool.remaining() < size;
    }

    public boolean isLastAllocated(ByteBuffer oldByteBuffer) {
      return lastAllocatedIndex > 0 && allocationHistory[lastAllocatedIndex - 1] == oldByteBuffer;
    }

    @Override
    public ByteBuffer reduceLastAllocated(ByteBuffer byteBuffer) {
      final ByteBuffer oldLastAllocated = (ByteBuffer) allocationHistory[lastAllocatedIndex - 1];

      pool.position(pool.position() - (oldLastAllocated.capacity() - byteBuffer.capacity()));
      allocationHistory[lastAllocatedIndex - 1] = byteBuffer;

      return oldLastAllocated;
    }

    @Override
    public int remaining() {
      return pool != null ? pool.remaining() : 0;
    }

    @Override
    public boolean hasRemaining() {
      return remaining() > 0;
    }

    @Override
    public int getMaxBufferSize() {
      return maxBufferSize;
    }

    @Override
    public void dispose() {
      // Nothing to do.
    }

    private ByteBuffer addHistory(ByteBuffer allocated) {
      if (lastAllocatedIndex >= allocationHistory.length) {
        allocationHistory = Arrays.copyOf(allocationHistory, allocationHistory.length * 3 / 2 + 1);
      }

      allocationHistory[lastAllocatedIndex++] = allocated;
      return allocated;
    }

    @Override
    public String toString() {
      return "(pool=" + pool + " last-allocated-index=" + (lastAllocatedIndex - 1) + " allocation-history="
          + Arrays.toString(allocationHistory) + ')';
    }

  }

}
