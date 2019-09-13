/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;

/**
 * {@link ManagedCursorProvider} implementation for {@link CursorIteratorProvider} instances
 *
 * @since 4.0
 */
public class ManagedCursorIteratorProvider extends ManagedCursorProvider<CursorIterator> implements CursorIteratorProvider {

  /**
   * {@inheritDoc}
   */
  public ManagedCursorIteratorProvider(CursorProvider<CursorIterator> delegate, MutableStreamingStatistics statistics) {
    super(delegate, statistics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorIterator managedCursor(CursorIterator cursor) {
    return new ManagedCursorIterator(this, cursor, getJanitor());
  }

}
