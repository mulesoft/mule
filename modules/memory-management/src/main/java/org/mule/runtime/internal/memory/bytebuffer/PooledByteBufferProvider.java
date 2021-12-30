/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import org.mule.runtime.api.memory.provider.ByteBufferProvider;

import java.nio.ByteBuffer;

public abstract class PooledByteBufferProvider implements ByteBufferProvider<ByteBuffer> {

  private final Pool[] pools;

  PooledByteBufferProvider(final int baseBufferSize, final int numberOfPools, final int growthFactor,
                           final float percentOfHeap, final float percentPreallocated) {
    if (baseBufferSize <= 0) {
      throw new IllegalArgumentException("baseBufferSize must be greater than zero");
    }
    if (numberOfPools <= 0) {
      throw new IllegalArgumentException("numberOfPools must be greater than zero");
    }
    if (growthFactor == 0 && numberOfPools > 1) {
      throw new IllegalArgumentException("if numberOfPools is greater than 0 - growthFactor must be greater than zero");
    }
    if (growthFactor < 0) {
      throw new IllegalArgumentException("growthFactor must be greater or equal to zero");
    }

    if (!isPowerOfTwo(baseBufferSize) || !isPowerOfTwo(growthFactor)) {
      throw new IllegalArgumentException("minBufferSize and growthFactor must be a power of two");
    }

    if (percentOfHeap <= 0.0f || percentOfHeap >= 1.0f) {
      throw new IllegalArgumentException("percentOfHeap must be greater than zero and less than 1");
    }

    if (percentPreallocated < 0.0f || percentPreallocated > 1.0f) {
      throw new IllegalArgumentException("percentPreallocated must be greater or equal to zero and less or equal to 1");
    }

    pools = new Pool[numberOfPools];
    for (int i = 0, bufferSize = baseBufferSize; i < numberOfPools; i++, bufferSize <<= growthFactor) {
      pools[i] = new Pool(bufferSize, percentPreallocated);
    }


  }

  private static boolean isPowerOfTwo(final int valueToCheck) {
    return (valueToCheck & valueToCheck - 1) == 0;
  }

  @Override
  public ByteBuffer allocate(int size) {
    return null;
  }

  @Override
  public ByteBuffer allocateAtLeast(int size) {
    return null;
  }

  @Override
  public ByteBuffer reallocate(ByteBuffer oldBuffer, int newSize) {
    return null;
  }

  @Override
  public void release(ByteBuffer buffer) {

  }

  @Override
  public byte[] getByteArray(int size) {
    return new byte[0];
  }

  @Override
  public void dispose() {

  }

  abstract boolean isDirect();

  private static final class Pool implements ByteBufferProvider<ByteBuffer> {

    private final int bufferSize;
    private final float percentPreallocated;

    public Pool(int bufferSize, float percentPreallocated) {
      this.bufferSize = bufferSize;
      this.percentPreallocated = percentPreallocated;
    }

    @Override
    public ByteBuffer allocate(int size) {
      return null;
    }

    @Override
    public ByteBuffer allocateAtLeast(int size) {
      return null;
    }

    @Override
    public ByteBuffer reallocate(ByteBuffer oldBuffer, int newSize) {
      return null;
    }

    @Override
    public void release(ByteBuffer buffer) {

    }

    @Override
    public byte[] getByteArray(int size) {
      return new byte[0];
    }

    @Override
    public void dispose() {

    }
  }
}
