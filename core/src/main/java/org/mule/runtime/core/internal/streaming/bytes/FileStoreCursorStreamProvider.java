/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.streaming.bytes.FileStoreCursorStreamConfig;

import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An implementation of {@link AbstractCursorStreamProviderAdapter} which yields
 * cursors that use disk for buffering
 *
 * @since 4.0
 */
public class FileStoreCursorStreamProvider extends AbstractCursorStreamProviderAdapter {

  private final int bufferSize;
  private final SwitchingInputStreamBuffer buffer;

  /**
   * Creates a new instance
   *
   * @param wrappedStream   the stream to buffer from
   * @param config          the config for the file store buffer
   * @param event           the {@link Event} on which buffering is taking place
   * @param bufferManager   the {@link ByteBufferManager} that will be used to allocate all buffers
   * @param executorService a {@link ScheduledExecutorService} for performing async tasks
   */
  public FileStoreCursorStreamProvider(InputStream wrappedStream,
                                       FileStoreCursorStreamConfig config,
                                       Event event,
                                       ByteBufferManager bufferManager,
                                       ScheduledExecutorService executorService) {
    super(wrappedStream, bufferManager, event);
    this.bufferSize = config.getMaxInMemorySize().toBytes();
    buffer = SwitchingInputStreamBuffer.of(wrappedStream, config, bufferManager, executorService);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorStream doOpenCursor() {
    return new BufferedCursorStream(buffer, getBufferManager(), bufferSize, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void releaseResources() {
    buffer.close();
  }
}
