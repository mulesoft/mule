/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.StreamingStrategy;

/**
 * Interface that is used for getting different {@link CursorStreamProviderFactory} that are suitable for a
 * {@link StreamingStrategy}.
 * 
 * @since 4.2.0
 */
@NoImplement
public interface ByteStreamingStrategyManager {

  /**
   * Returns a suitable {@link CursorStreamProviderFactory} with its default configuration for a given {@link StreamingStrategy}
   * 
   * @param streamingStrategy the {@link StreamingStrategy} we want to get a {@link CursorStreamProviderFactory} for.
   * @return the default {@link CursorStreamProviderFactory} for the streamingStrategy given.
   */
  public CursorStreamProviderFactory getDefaultCursorProviderFactory(StreamingStrategy streamingStrategy);

}
