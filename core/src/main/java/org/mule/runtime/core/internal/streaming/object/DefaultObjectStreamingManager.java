/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.core.internal.streaming.object.factory.InMemoryCursorIteratorProviderFactory;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;

/**
 * Default implementation of {@link ObjectStreamingManager}
 *
 * @since 4.0
 */
public class DefaultObjectStreamingManager implements ObjectStreamingManager {

  protected final StreamingManager streamingManager;

  public DefaultObjectStreamingManager(StreamingManager streamingManager) {
    this.streamingManager = streamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig config) {
    return new InMemoryCursorIteratorProviderFactory(config, streamingManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getNullCursorProviderFactory() {
    return new NullCursorIteratorProviderFactory(streamingManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getDefaultCursorProviderFactory() {
    return getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig.getDefault());
  }
}
