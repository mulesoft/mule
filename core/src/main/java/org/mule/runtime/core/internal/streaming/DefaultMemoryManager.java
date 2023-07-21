/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
