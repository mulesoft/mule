/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
