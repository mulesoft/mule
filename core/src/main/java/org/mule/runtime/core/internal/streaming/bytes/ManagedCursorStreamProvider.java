/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;

/**
 * {@link ManagedCursorProvider} implementation for {@link CursorStreamProvider} instances
 *
 * @since 4.0
 */
public class ManagedCursorStreamProvider extends ManagedCursorProvider<CursorStream> implements CursorStreamProvider {

  /**
   * {@inheritDoc}
   */
  public ManagedCursorStreamProvider(CursorProvider<CursorStream> delegate, MutableStreamingStatistics statistics) {
    super(delegate, statistics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorStream managedCursor(CursorStream cursor) {
    return new ManagedCursorStreamDecorator(this, cursor, getJanitor());
  }
}
