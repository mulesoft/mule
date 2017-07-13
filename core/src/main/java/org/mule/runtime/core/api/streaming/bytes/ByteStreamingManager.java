/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

/**
 * Manages components in charge of streaming bytes so that the runtime can keep track of them,
 * enforce policies and make sure that all resources are reclaimed once no longer needed.
 *
 * @since 4.0
 */
public interface ByteStreamingManager {

  /**
   * Creates a {@link CursorStreamProviderFactory} which buffers in memory
   *
   * @param config the configuration for the produced {@link CursorStreamProvider} instances
   * @return a new {@link CursorStreamProviderFactory}
   */
  CursorStreamProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorStreamConfig config);

  /**
   * Creates a null object implementation of {@link CursorStreamProviderFactory}
   *
   * @return a new {@link CursorStreamProviderFactory}
   */
  CursorStreamProviderFactory getNullCursorProviderFactory();

  /**
   * @return The default implementation of {@link CursorStreamProviderFactory}
   */
  CursorStreamProviderFactory getDefaultCursorProviderFactory();
}
