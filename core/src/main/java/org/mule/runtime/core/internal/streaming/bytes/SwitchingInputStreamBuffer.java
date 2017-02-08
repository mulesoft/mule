/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An implementation of {@link AbstractInputStreamBuffer} which allows switching the buffering strategy.
 * <p>
 * It starts using a {@link AbstractInputStreamBuffer delegate}. Once (and if) that delegate achieves its maximum buffer size
 * and can no longer be expanded, all the data in that buffer is moved to a {@link FileStoreInputStreamBuffer} which continues
 * the buffering operation.
 *
 * @since 4.0
 */
public class SwitchingInputStreamBuffer extends AbstractInputStreamBuffer {

  private AbstractInputStreamBuffer delegate;
  private final FileStoreCursorStreamConfig config;
  private final ScheduledExecutorService executorService;

  public static SwitchingInputStreamBuffer of(InputStream stream,
                                              FileStoreCursorStreamConfig config,
                                              ScheduledExecutorService executorService) {
    InMemoryStreamBuffer delegate =
        new InMemoryStreamBuffer(stream, new InMemoryCursorStreamConfig(config.getMaxInMemorySize(), null, null));

    return new SwitchingInputStreamBuffer(stream, delegate, config, executorService);
  }

  /**
   * Creates a new instance
   *
   * @param stream           the stream to buffer
   * @param delegate         the delegate for the initial buffering
   * @param config           the config of the {@link FileStoreCursorStreamConfig} in case of the delegate been exhausted.
   * @param executorService a {@link ScheduledExecutorService} for performing asynchronous tasks
   */
  private SwitchingInputStreamBuffer(InputStream stream,
                                     AbstractInputStreamBuffer delegate,
                                     FileStoreCursorStreamConfig config,
                                     ScheduledExecutorService executorService) {
    super(stream, openStreamChannel(stream), delegate.getBuffer());
    this.delegate = delegate;
    this.config = config;
    this.executorService = executorService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doClose() {
    delegate.doClose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getBackwardsData(ByteBuffer dest, Range requiredRange, int length) {
    return delegate.getBackwardsData(dest, requiredRange, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int consumeForwardData(ByteBuffer buffer) throws IOException {
    if (!buffer.hasRemaining() && canBeExpanded()) {
      switchDelegate();
    }
    return delegate.consumeForwardData(buffer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean canBeExpanded() {
    return delegate instanceof InMemoryStreamBuffer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void acquireBufferLock() {
    super.acquireBufferLock();
    try {
      delegate.acquireBufferLock();
    } catch (Exception e) {
      super.releaseBufferLock();
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void releaseBufferLock() {
    try {
      super.releaseBufferLock();
    } finally {
      delegate.releaseBufferLock();
    }
  }

  private void switchDelegate() throws IOException {
    ByteBuffer buffer = getBuffer();
    buffer.position(0);

    AbstractInputStreamBuffer newDelegate = new FileStoreInputStreamBuffer(getStream(),
                                                                           getStreamChannel(),
                                                                           config,
                                                                           buffer,
                                                                           executorService);
    AbstractInputStreamBuffer oldDelegate = delegate;
    delegate = newDelegate;

    try {
      oldDelegate.releaseBufferLock();
      oldDelegate.yieldStream();
      oldDelegate.close();
    } catch (Exception e) {
      newDelegate.close();
      if (e instanceof IOException) {
        throw (IOException) e;
      } else {
        throw new MuleRuntimeException(e);
      }
    }
  }
}
