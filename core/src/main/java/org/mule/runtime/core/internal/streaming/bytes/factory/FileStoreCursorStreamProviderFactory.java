/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes.factory;

import static org.mule.runtime.core.api.functional.Either.left;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.internal.streaming.bytes.ByteStreamingManagerAdapter;
import org.mule.runtime.core.internal.streaming.bytes.CursorStreamProviderAdapter;
import org.mule.runtime.core.internal.streaming.bytes.FileStoreCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.bytes.InMemoryCursorStreamProvider;

import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An implementation of {@link AbstractCursorStreamProviderFactory} which always
 * generates instances of {@link FileStoreCursorStreamProvider}
 *
 * @see FileStoreCursorStreamProvider
 * @since 4.0
 */
public class FileStoreCursorStreamProviderFactory extends AbstractCursorStreamProviderFactory {

  private final FileStoreCursorStreamConfig config;
  private final ScheduledExecutorService executorService;

  /**
   * Creates a new instance
   *
   * @param streamingManager the manager which will track the produced providers.
   * @param config           the config for the generated providers
   * @param executorService a {@link ScheduledExecutorService} for executing asynchronous tasks
   */
  public FileStoreCursorStreamProviderFactory(ByteStreamingManagerAdapter streamingManager,
                                              FileStoreCursorStreamConfig config,
                                              ScheduledExecutorService executorService) {
    super(streamingManager);
    this.config = config;
    this.executorService = executorService;
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@link InMemoryCursorStreamProvider} wrapped in an {@link Either}
   */
  @Override
  protected Either<CursorStreamProviderAdapter, InputStream> resolve(InputStream inputStream, Event event) {
    return left(new FileStoreCursorStreamProvider(inputStream, config, event, executorService));
  }
}
