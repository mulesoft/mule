/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

/**
 * Handles the runtime's memory
 *
 * @since 4.0
 */
public interface MemoryManager {

  /**
   * Returns the maximum amount of memory that the runtime will attempt to use, following the same semantics as
   * {@link Runtime#totalMemory()}
   */
  long getMaxMemory();
}


