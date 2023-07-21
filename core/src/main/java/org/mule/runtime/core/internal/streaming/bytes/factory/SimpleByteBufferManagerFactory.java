/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes.factory;

import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManagerFactory;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;

public class SimpleByteBufferManagerFactory implements ByteBufferManagerFactory {

  @Override
  public ByteBufferManager create() {
    return new SimpleByteBufferManager();
  }
}
