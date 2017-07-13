/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

/**
 * Default implementation of {@link MemoryManager}
 *
 * @since 4.0
 */
public class DefaultMemoryManager implements MemoryManager {

  /**
   * {@inheritDoc}
   */
  @Override
  public long getMaxMemory() {
    return Runtime.getRuntime().maxMemory();
  }
}
