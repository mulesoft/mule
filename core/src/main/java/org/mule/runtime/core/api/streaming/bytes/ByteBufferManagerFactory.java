/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

/**
 * Factory for instances of {@link ByteBufferManager}.
 * <p>
 * Implementations are required to have a default constructor
 *
 * @since 4.3.0
 */
public interface ByteBufferManagerFactory {

  /**
   * @return a new {@link ByteBufferManager}
   */
  ByteBufferManager create();
}
