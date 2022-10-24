/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

import java.util.Set;

/**
 * An exporter for {@link InternalSpanExporter}
 *
 * @since 4.5.0
 */
public interface InternalSpanExporter {

  /**
   * Exports {@link InternalSpan}'s.
   *
   * @param internalSpan the {@link InternalSpan} to export
   */
  void export(InternalSpan internalSpan);

  /**
   * @param internalSpanExporterVisitor the visitor
   * @param <T>                         the result type
   * @return the result of visit
   */
  <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor);

  /**
   * Indicates that no children will be exported till a span is found with the names returned.
   *
   * For example: in case noExportUntil returns "execute-next", no children will be exported till an execute-next span.
   *
   * ------------- span (exported) --------------------------------------------------------- |___ logger (not exported) ____ |___
   * scope (not exported) |___ execute-next (exported) |__ flow (exported)
   *
   * @return the name of the spans where the span hierarchy is exported again.
   */
  Set<String> noExportUntil();

  /**
   * Informs the exporter that the name of the span was updated.
   *
   * @param name the name to set to the current span.
   */
  default void onNameUpdated(String newName) {
    // Nothing to do by default.
  }
}
