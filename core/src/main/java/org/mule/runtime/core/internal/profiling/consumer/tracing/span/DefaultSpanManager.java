/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import static java.util.concurrent.TimeUnit.MINUTES;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;

public class DefaultSpanManager implements SpanManager {

  // TODO W-10930532: a removal listener has to be added to end the span in case it is not closed by the runtime.
  private static final Cache<SpanIdentifier, InternalSpan> cache =
      newBuilder().weakValues().expireAfterAccess(60, MINUTES).build();

  @Override
  public InternalSpan getSpan(SpanIdentifier identifier, Function<SpanIdentifier, InternalSpan> spanCreator) {
    return cache.get(identifier, spanCreator);
  }

  @Override
  public InternalSpan getSpanIfPresent(SpanIdentifier identifier) {
    return cache.getIfPresent(identifier);
  }

  @Override
  public void removeSpan(Span span) {
    cache.invalidate(span);
  }
}
