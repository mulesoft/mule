/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

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
}
