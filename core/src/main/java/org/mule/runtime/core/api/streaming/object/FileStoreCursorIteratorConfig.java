/*
 * (c) 2003-2021 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.runtime.core.api.streaming.object;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE;

public class FileStoreCursorIteratorConfig {

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
