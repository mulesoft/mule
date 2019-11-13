/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes.factory;

import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManagerFactory;
import org.mule.runtime.core.internal.streaming.bytes.PoolingByteBufferManager;

public class PoolingByteBufferManagerFactory implements ByteBufferManagerFactory {

  @Override
  public ByteBufferManager create() {
    return new PoolingByteBufferManager();
  }
}
