/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.function.Function;

/**
 * Manages internal runtime spans.
 *
 * @since 4.5.0
 */
public interface SpanManager {

  /**
   * Gets the span corresponding to the identifier and creates one if it does not exist.
   *
   * @param identifier  the identifier.
   * @param spanCreator function to create a span.
   * @return the {@link Span} corresponding to the identifier.
   */
  InternalSpan getSpan(SpanIdentifier identifier, Function<SpanIdentifier, InternalSpan> spanCreator);

  /**
   * Gets the span corresponding to the identifier if present.
   *
   * @param identifier the identifier.
   * @return the {@link Span} corresponding to the identifier or null otherwise.
   */
  InternalSpan getSpanIfPresent(SpanIdentifier identifier);

  /**
   * Removes the span corresponding to the identifier.
   *
   * @param span the span to be removed.
   * @return the {@link Span} that was removed.
   */
  void removeSpan(Span span);

}
