/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.api.memory.provider.type.ByteBufferPoolStrategy.FIXED_BUFFERS_POOL;
import static org.mule.runtime.api.memory.provider.type.ByteBufferType.DIRECT;
import static org.mule.runtime.internal.memory.profiling.NoOpMemoryProfilingService.getNoOpMemoryProfilingService;

import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.profiling.ProfilingService;

import java.nio.ByteBuffer;

/**
 * A Builder for a {@link ByteBufferProvider<ByteBuffer>}
 *
 * @since 4.5.0
 */
public final class ByteBufferProviderBuilder {

  public static final String BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE = "Byte buffer provider name cannot be null.";
  public static final String PROFILING_SERVICE_CANNOT_BE_NULL_MESSAGE = "Profiling service cannot be null.";
  private final boolean isDirect;

  private ByteBufferPoolConfiguration poolConfiguration;
  private ProfilingService profilingService = getNoOpMemoryProfilingService();
  private String name;

  private ByteBufferProviderBuilder(boolean isDirect) {
    this.isDirect = isDirect;
  }

  public static ByteBufferProviderBuilder buildByteBufferProviderFrom(ByteBufferType byteBufferType) {
    return new ByteBufferProviderBuilder(DIRECT.equals(byteBufferType));
  }

  public ByteBufferProvider<ByteBuffer> build() {
    if (profilingService == null) {
      throw new IllegalArgumentException(PROFILING_SERVICE_CANNOT_BE_NULL_MESSAGE);
    }

    if (name == null) {
      throw new IllegalArgumentException(BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE);
    }

    if (isDirect) {
      if (poolConfiguration != null) {
        if (poolConfiguration.getByteBufferPoolStrategy() == FIXED_BUFFERS_POOL) {
          return new WeavePoolBasedByteBufferProvider(name, poolConfiguration.getMaxBufferSize(),
                                                      poolConfiguration.getNumberOfPools(), profilingService);
        }
        return new DirectByteBufferProvider(name, poolConfiguration.getMaxBufferSize(), poolConfiguration.getBaseByteBufferSize(),
                                            poolConfiguration.getGrowthFactor(), poolConfiguration.getNumberOfPools(),
                                            profilingService);
      } else {
        return new DirectByteBufferProvider(name, profilingService);
      }
    } else {
      if (poolConfiguration != null) {
        return new HeapByteBufferProvider(name, poolConfiguration.getMaxBufferSize(), poolConfiguration.getBaseByteBufferSize(),
                                          poolConfiguration.getGrowthFactor(), poolConfiguration.getNumberOfPools(),
                                          profilingService);
      } else {
        return new HeapByteBufferProvider(name, profilingService);
      }
    }
  }

  public ByteBufferProviderBuilder withPoolConfiguration(ByteBufferPoolConfiguration poolConfiguration) {
    this.poolConfiguration = poolConfiguration;
    return this;
  }

  public ByteBufferProviderBuilder withProfilingService(ProfilingService profilingService) {
    this.profilingService = profilingService;
    return this;
  }

  public ByteBufferProviderBuilder withName(String name) {
    this.name = name;
    return this;
  }
}
