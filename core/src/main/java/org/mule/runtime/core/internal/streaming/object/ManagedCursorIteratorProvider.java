/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.IdentifiableCursorProvider;
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
  public ManagedCursorIteratorProvider(IdentifiableCursorProvider<CursorIterator> delegate,
                                       MutableStreamingStatistics statistics) {
    super(delegate, statistics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorIterator managedCursor(CursorIterator cursor) {
    return new ManagedCursorIterator(this, cursor, getJanitor());
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
