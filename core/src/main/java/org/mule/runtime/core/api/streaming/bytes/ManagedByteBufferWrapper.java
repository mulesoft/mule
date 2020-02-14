/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.core.api.util.func.CheckedConsumer;

import java.nio.ByteBuffer;

public class ManagedByteBufferWrapper {

  private final ByteBuffer delegate;
  private final CheckedConsumer<ManagedByteBufferWrapper> deallocator;

  public ManagedByteBufferWrapper(ByteBuffer delegate, CheckedConsumer<ManagedByteBufferWrapper> deallocator) {
    this.delegate = delegate;
    this.deallocator = deallocator;
  }

  public ByteBuffer getDelegate() {
    return delegate;
  }

  public void deallocate() {
    deallocator.accept(this);
  }
}
