/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

/**
 * Handles the runtime's memory
 *
 * @since 4.0
 */
public interface MemoryManager {

  /**
   * Returns the maximum amount of memory that the runtime will attempt to use,
   * following the same semantics as {@link Runtime#totalMemory()}
   */
  long getMaxMemory();
}


