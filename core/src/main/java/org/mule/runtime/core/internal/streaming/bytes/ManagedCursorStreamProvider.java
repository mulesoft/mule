/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.IdentifiableCursorProvider;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;

/**
 * {@link ManagedCursorProvider} implementation for {@link CursorStreamProvider} instances
 *
 * @since 4.0
 */
public class ManagedCursorStreamProvider extends ManagedCursorProvider<CursorStream> implements CursorStreamProvider {

  /**
   * {@link ManagedCursorProvider} implementation for {@link CursorStreamProvider} instances
   *
   * @param delegate
   * @param statistics
   */
  public ManagedCursorStreamProvider(IdentifiableCursorProvider<CursorStream> delegate,
                                     MutableStreamingStatistics statistics) {
    super(delegate, statistics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorStream managedCursor(CursorStream cursor) {
    return new ManagedCursorStreamDecorator(this, cursor, getJanitor());
  }

  public boolean isManaged() {
    return true;
  }
}
