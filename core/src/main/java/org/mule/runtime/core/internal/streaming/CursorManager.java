/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.STREAMING_VERBOSE_PROPERTY;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;

import static java.lang.Boolean.getBoolean;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;

/**
 * Keeps track of active {@link Cursor cursors} and their {@link CursorProvider providers}
 *
 * @since 4.0
 */
public class CursorManager {

  public static final boolean STREAMING_VERBOSE = getBoolean(STREAMING_VERBOSE_PROPERTY);

  private final MutableStreamingStatistics statistics;
  private final StreamingGhostBuster ghostBuster;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   */
  public CursorManager(MutableStreamingStatistics statistics, StreamingGhostBuster ghostBuster) {
    this.statistics = statistics instanceof NullStreamingStatistics ? null : statistics;
    this.ghostBuster = requireNonNull(ghostBuster);
  }

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider     the provider to be tracked
   * @param ownerContext the root context of the event that created the provider
   * @return a {@link CursorProvider}
   */
  public CursorProvider manage(final CursorProvider provider, DefaultEventContext ownerContext) {
    CursorProvider innerDelegate = unwrap(provider);
    IdentifiableCursorProvider identifiable = IdentifiableCursorProviderDecorator.of(provider);

    ManagedCursorProvider managedProvider;
    if (innerDelegate instanceof CursorStreamProvider) {
      managedProvider = new ManagedCursorStreamProvider(identifiable, statistics);
    } else if (innerDelegate instanceof CursorIteratorProvider) {
      managedProvider = new ManagedCursorIteratorProvider(identifiable, statistics);
    } else {
      throw new MuleRuntimeException(createStaticMessage("Unknown cursor provider type: " + innerDelegate.getClass().getName()));
    }

    return ownerContext.track(managedProvider, ghostBuster);
  }
}
