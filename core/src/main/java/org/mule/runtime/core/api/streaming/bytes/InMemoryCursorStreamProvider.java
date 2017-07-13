/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.internal.streaming.bytes.BufferedCursorStream;
import org.mule.runtime.core.internal.streaming.bytes.InMemoryStreamBuffer;

import java.io.InputStream;

/**
 * An implementation of {@link AbstractCursorStreamProvider} which yields
 * cursors that only use memory for buffering
 *
 * @since 4.0
 */
public class InMemoryCursorStreamProvider extends AbstractCursorStreamProvider {

  private final InMemoryStreamBuffer buffer;

  /**
   * Creates a new instance
   *
   * @param wrappedStream the stream to buffer from
   * @param config        the config of the generated buffer
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public InMemoryCursorStreamProvider(InputStream wrappedStream,
                                      InMemoryCursorStreamConfig config,
                                      ByteBufferManager bufferManager) {
    super(wrappedStream);
    buffer = new InMemoryStreamBuffer(wrappedStream, config, bufferManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorStream doOpenCursor() {
    return new BufferedCursorStream(buffer, this);
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
