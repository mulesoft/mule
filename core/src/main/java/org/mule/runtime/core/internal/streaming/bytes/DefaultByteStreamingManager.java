/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.factory.InMemoryCursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;

/**
 * Default implementation of {@link ByteStreamingManager}
 *
 * @since 4.0
 */
public class DefaultByteStreamingManager implements ByteStreamingManager {

  private final ByteBufferManager bufferManager;
  protected final StreamingManager streamingManager;

  public DefaultByteStreamingManager(ByteBufferManager bufferManager, StreamingManager streamingManager) {
    this.bufferManager = bufferManager;
    this.streamingManager = streamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorStreamConfig config) {
    return new InMemoryCursorStreamProviderFactory(bufferManager, config, streamingManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getNullCursorProviderFactory() {
    return new NullCursorStreamProviderFactory(bufferManager, streamingManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getDefaultCursorProviderFactory() {
    return new InMemoryCursorStreamProviderFactory(bufferManager, InMemoryCursorStreamConfig.getDefault(), streamingManager);
  }

  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }
}
