/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.core.api.streaming.bytes.ManagedByteBufferWrapper;
import org.mule.runtime.core.internal.streaming.MemoryManager;

/**
 * Basic implementation of {@link MemoryBoundByteBufferManager}
 *
 * @since 4.3.0
 */
public class SimpleByteBufferManager extends MemoryBoundByteBufferManager {

  public SimpleByteBufferManager() {}

  public SimpleByteBufferManager(MemoryManager memoryManager) {
    super(memoryManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedByteBufferWrapper allocateManaged(int capacity) {
    return new ManagedByteBufferWrapper(allocateIfFits(capacity), buffer -> doDeallocate(buffer.getDelegate()));
  }
}
