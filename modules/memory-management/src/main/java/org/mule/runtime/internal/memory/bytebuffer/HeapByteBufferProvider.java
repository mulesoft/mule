/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import java.nio.ByteBuffer;

/**
 * A {@link org.mule.runtime.api.memory.provider.ByteBufferProvider} implementation that can be used to retrieve heap
 * {@link ByteBuffer}'s.
 *
 * Based on Grizzly Implementation.
 *
 * @since 4.5.0
 */
public class HeapByteBufferProvider extends ThreadPoolBasedByteBufferProvider {

  public HeapByteBufferProvider() {
    super();
  }

  public HeapByteBufferProvider(int maxSize, int baseByteBufferSize, int growthFactor, int numberOfPools) {
    super(maxSize, baseByteBufferSize, growthFactor, numberOfPools);
  }

  @Override
  protected ByteBuffer doAllocate(int size) {
    return ByteBuffer.allocate(size);
  }
}
