/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.streaming;

import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;

import java.nio.ByteBuffer;

/**
 * Simple implementation of {@link ByteBufferManager}
 *
 * @since 4.0
 */
public class SimpleByteBufferManager implements ByteBufferManager {

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteBuffer allocate(int capacity) {
    return ByteBuffer.allocate(capacity);
  }

  /**
   * No - Op operation
   * {@inheritDoc}
   */
  @Override
  public void deallocate(ByteBuffer byteBuffer) {

  }
}
