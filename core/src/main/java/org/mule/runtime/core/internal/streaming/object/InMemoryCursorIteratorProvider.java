/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;

import java.util.Iterator;

/**
 * An implementation of {@link AbstractCursorIteratorProvider} which yields cursors that only use memory for buffering
 *
 * @since 4.0
 */
public class InMemoryCursorIteratorProvider extends AbstractCursorIteratorProvider {

  private final ObjectStreamBuffer buffer;

  /**
   * Creates a new instance
   *
   * @param stream              the stream to buffer from
   * @param config              the config of the generated buffer
   * @param originatingLocation indicates where the cursor was created
   *
   * @since 4.3.0
   */
  public InMemoryCursorIteratorProvider(Iterator stream, InMemoryCursorIteratorConfig config,
                                        ComponentLocation originatingLocation, boolean trackCursorProviderClose) {
    super(stream, originatingLocation, trackCursorProviderClose);
    buffer = new InMemoryObjectStreamBuffer(stream, config);
    buffer.initialise();
  }

  /**
   * Creates a new instance
   *
   * @param stream the stream to buffer from
   * @param config the config of the generated buffer
   * @deprecated On 4.3.0, please use
   *             {@link #InMemoryCursorIteratorProvider(Iterator, InMemoryCursorIteratorConfig, ComponentLocation, boolean)}
   *             instead.
   */
  public InMemoryCursorIteratorProvider(Iterator stream, InMemoryCursorIteratorConfig config) {
    this(stream, config, null, false);
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
