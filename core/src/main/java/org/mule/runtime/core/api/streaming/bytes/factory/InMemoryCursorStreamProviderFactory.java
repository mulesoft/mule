/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes.factory;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;

import java.io.InputStream;

/**
 * An implementation of {@link AbstractCursorStreamProviderFactory} which always
 * generates instances of {@link InMemoryCursorStreamProvider}
 *
 * @see InMemoryCursorStreamProvider
 * @since 4.0
 */
public class InMemoryCursorStreamProviderFactory extends AbstractCursorStreamProviderFactory {

  private final InMemoryCursorStreamConfig config;

  /**
   * Creates a new instance
   *
   * @param config           the config for the generated providers
   * @param bufferManager    the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public InMemoryCursorStreamProviderFactory(ByteBufferManager bufferManager,
                                             InMemoryCursorStreamConfig config,
                                             StreamingManager streamingManager) {
    super(bufferManager, streamingManager);
    this.config = config;
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@link InMemoryCursorStreamProvider} wrapped in an {@link Either}
   */
  @Override
  protected Object resolve(InputStream inputStream, CoreEvent event) {
    InMemoryCursorStreamProvider inMemoryCursorStreamProvider =
        new InMemoryCursorStreamProvider(inputStream, config, getBufferManager());
    inMemoryCursorStreamProvider.setAnnotations(getAnnotations());
    return inMemoryCursorStreamProvider;
  }
}
