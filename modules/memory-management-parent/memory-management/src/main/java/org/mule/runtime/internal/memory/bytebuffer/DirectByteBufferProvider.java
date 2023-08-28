/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import org.mule.runtime.api.profiling.ProfilingService;

import static java.nio.ByteBuffer.allocateDirect;

import java.nio.ByteBuffer;

/**
 * A {@link org.mule.runtime.api.memory.provider.ByteBufferProvider} implementation that can be used to retrieve direct
 * {@link ByteBuffer}'s.
 *
 * Based on Grizzly Implementation.
 *
 * @since 4.5.0
 */
public class DirectByteBufferProvider extends ThreadPoolBasedByteBufferProvider {

  public DirectByteBufferProvider(String name, ProfilingService profilingService) {
    super(name, profilingService);
  }

  public DirectByteBufferProvider(String name, int maxSize, int baseByteBufferSize, int growthFactor, int numberOfPools,
                                  ProfilingService profilingService) {
    super(name, maxSize, baseByteBufferSize, growthFactor, numberOfPools, profilingService);
  }

  @Override
  protected ByteBuffer doAllocate(int size) {
    return allocateDirect(size);
  }
}
