/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.api.memory.provider.type.ByteBufferType.DIRECT;

import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;

import java.nio.ByteBuffer;

/**
 * A Builder for a {@link ByteBufferProvider<ByteBuffer>}
 *
 * @since 4.5.0
 */
public class ByteBufferProviderBuilder {

  private final boolean isDirect;

  private ByteBufferPoolConfiguration poolConfiguration;

  private int maxBufferSize;

  private ByteBufferProviderBuilder(boolean isDirect) {
    this.isDirect = isDirect;
  }

  public static ByteBufferProviderBuilder buildByteBufferProviderFrom(ByteBufferType byteBufferType) {
    return new ByteBufferProviderBuilder(byteBufferType.equals(DIRECT));
  }

  public ByteBufferProvider<ByteBuffer> build() {
    if (isDirect) {
      if (poolConfiguration != null) {
        return new DirectByteBufferProvider(maxBufferSize, poolConfiguration.getBaseByteBufferSize(),
                                            poolConfiguration.getGrowthFactor(), poolConfiguration.getNumberOfPools());
      } else {
        return new DirectByteBufferProvider();
      }
    } else {
      if (poolConfiguration != null) {
        return new HeapByteBufferProvider(maxBufferSize, poolConfiguration.getBaseByteBufferSize(),
                                          poolConfiguration.getGrowthFactor(), poolConfiguration.getNumberOfPools());
      } else {
        return new HeapByteBufferProvider();
      }
    }
  }


  public ByteBufferProviderBuilder withPoolConfiguration(ByteBufferPoolConfiguration poolConfiguration) {
    this.poolConfiguration = poolConfiguration;
    return this;
  }

  public ByteBufferProviderBuilder withMaxBufferSize(int maxBufferSize) {
    this.maxBufferSize = maxBufferSize;
    return this;
  }
}
