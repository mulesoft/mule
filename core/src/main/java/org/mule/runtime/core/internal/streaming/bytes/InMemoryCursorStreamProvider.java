/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;

import java.io.InputStream;

/**
 * An implementation of {@link AbstractCursorStreamProviderAdapter} which yields
 * cursors that only use memory for buffering
 *
 * @since 4.0
 */
public class InMemoryCursorStreamProvider extends AbstractCursorStreamProviderAdapter {

  private final InMemoryStreamBuffer buffer;
  private final int bufferSize;

  /**
   * Creates a new instance
   *
   * @param wrappedStream the stream to buffer from
   * @param config        the config of the generated buffer
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   * @param event         the {@link Event} in which streaming is taking place
   */
  public InMemoryCursorStreamProvider(InputStream wrappedStream,
                                      InMemoryCursorStreamConfig config,
                                      ByteBufferManager bufferManager,
                                      Event event) {
    super(wrappedStream, bufferManager, event);
    buffer = new InMemoryStreamBuffer(wrappedStream, config, bufferManager);
    bufferSize = config.getInitialBufferSize().toBytes();
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
    if (buffer != null) {
      buffer.close();
    }
  }
}
