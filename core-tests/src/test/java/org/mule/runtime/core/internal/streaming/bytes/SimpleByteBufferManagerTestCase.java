/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import org.mule.runtime.core.internal.streaming.MemoryManager;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;

@SmallTest
@Feature(STREAMING)
public class SimpleByteBufferManagerTestCase extends MemoryBoundByteBufferManagerContractTestCase {

  @Override
  protected MemoryBoundByteBufferManager createDefaultBoundBuffer() {
    return new SimpleByteBufferManager();
  }

  @Override
  protected MemoryBoundByteBufferManager createBuffer(MemoryManager memoryManager, int capacity) {
    return new SimpleByteBufferManager(memoryManager);
  }
}
