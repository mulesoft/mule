/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming.object;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

/**
 * Manages components in charge of streaming objects so that the runtime can keep track of them, enforce policies and make sure
 * that all resources are reclaimed once no longer needed.
 *
 * @since 4.0
 */
@NoImplement
public interface ObjectStreamingManager {

  /**
   * Creates a {@link CursorIteratorProviderFactory} which buffers in memory
   *
   * @param config the configuration for the produced {@link CursorIteratorProvider} instances
   * @return a new {@link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig config);

  /**
   * Creates a {@link CursorIteratorProviderFactory} which buffers in disk.
   * <p>
   * Functionality has been available since 4.0, but was made available through this interface in 4.5.0.
   *
   * @param config the configuration for the produced {@link CursorIteratorProvider} instances
   * @return a new {@link CursorIteratorProviderFactory}
   * @since 4.5.0
   */
  default CursorIteratorProviderFactory getFileStoreCursorIteratorProviderFactory(FileStoreCursorIteratorConfig config) {
    throw new UnsupportedOperationException("Only supported in EE edition");
  }

  /**
   * Creates a null object implementation of {@link CursorIteratorProviderFactory}
   *
   * @return a new {@link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getNullCursorProviderFactory();

  /**
   * @return The default implementation of {@link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getDefaultCursorProviderFactory();
}
