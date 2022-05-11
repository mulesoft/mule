/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
