/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;

/**
 * An implementation of {@link AbstractCursorIteratorProvider} which yields
 * cursors that only use memory for buffering
 *
 * @since 4.0
 */
public class InMemoryCursorIteratorProvider extends AbstractCursorIteratorProvider {

  private final ObjectStreamBuffer buffer;

  /**
   * Creates a new instance
   *
   * @param stream the stream to buffer from
   * @param config        the config of the generated buffer
   */
  public InMemoryCursorIteratorProvider(StreamingIterator stream, InMemoryCursorIteratorConfig config) {
    super(stream);
    buffer = new InMemoryObjectStreamBuffer(stream, config);
    buffer.initialise();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorIterator doOpenCursor() {
    return new BufferedCursorIterator(buffer, this);
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
