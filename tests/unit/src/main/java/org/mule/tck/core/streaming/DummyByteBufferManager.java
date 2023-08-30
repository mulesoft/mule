/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.streaming;

import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ManagedByteBufferWrapper;

import java.nio.ByteBuffer;

public class DummyByteBufferManager implements ByteBufferManager {

  @Override
  public ByteBuffer allocate(int capacity) {
    return ByteBuffer.allocate(capacity);
  }

  @Override
  public ManagedByteBufferWrapper allocateManaged(int capacity) {
    return new ManagedByteBufferWrapper(ByteBuffer.allocate(capacity), buffer -> {
    });
  }

  @Override
  public void deallocate(ByteBuffer byteBuffer) {

  }
}
