/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.internal.memory.bytebuffer.profiling;

import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;

/**
 * A default implementation of {@link org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext}
 */
public class DefaultByteBufferProviderEventContext implements ByteBufferProviderEventContext {

  private final String byteBufferProviderName;
  private final long triggerTimestamp;
  private final int size;

  public DefaultByteBufferProviderEventContext(String byteBufferProviderName, long triggerTimestamp, int size) {
    this.byteBufferProviderName = byteBufferProviderName;
    this.triggerTimestamp = triggerTimestamp;
    this.size = size;
  }

  @Override
  public long getTriggerTimestamp() {
    return triggerTimestamp;
  }

  @Override
  public String getByteBufferProviderName() {
    return byteBufferProviderName;
  }

  @Override
  public int size() {
    return size;
  }
}
