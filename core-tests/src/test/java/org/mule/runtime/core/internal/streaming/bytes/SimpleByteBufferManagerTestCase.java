/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
