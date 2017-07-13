/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

/**
 * Manages components in charge of streaming objects so that the runtime can keep track of them,
 * enforce policies and make sure that all resources are reclaimed once no longer needed.
 *
 * @since 4.0
 */
public interface ObjectStreamingManager {

  /**
   * Creates a {@link CursorIteratorProviderFactory} which buffers in memory
   *
   * @param config the configuration for the produced {@link CursorIteratorProvider} instances
   * @return a new {@link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig config);

  /**
   * Creates a null object implementation of {@link CursorIteratorProviderFactory}
   *
   * @return a new {@link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getNullCursorProviderFactory();

  /**
   * @return The default implementation of {@Link CursorIteratorProviderFactory}
   */
  CursorIteratorProviderFactory getDefaultCursorProviderFactory();
}
