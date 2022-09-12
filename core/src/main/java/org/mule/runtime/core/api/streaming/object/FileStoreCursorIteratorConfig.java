/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.object;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

/**
 * Configuration for FileStore based {@link CursorIteratorProvider} implementations.
 * <p>
 * This functionality has been available since 4.0, but only made available through this module since 4.5.0
 *
 * @since 4.5.0
 */
@NoInstantiate
public final class FileStoreCursorIteratorConfig {

  private final int maxInMemoryInstances;

  /**
   * @return A new instance configured with default settings
   */
  public static FileStoreCursorIteratorConfig getDefault() {
    return new FileStoreCursorIteratorConfig(DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE);
  }

  /**
   * Creates a new instance
   *
   * @param maxInMemoryInstances the maximum amount of space that the buffer can grow to. Use {@code null} for unbounded buffers
   * @throws IllegalArgumentException if any of the given arguments is invalid
   */
  public FileStoreCursorIteratorConfig(int maxInMemoryInstances) {
    checkArgument(maxInMemoryInstances > 0, "maxInMemoryInstances must be greater than zero");
    this.maxInMemoryInstances = maxInMemoryInstances;
  }

  public int getMaxInMemoryInstances() {
    return maxInMemoryInstances;
  }
}
