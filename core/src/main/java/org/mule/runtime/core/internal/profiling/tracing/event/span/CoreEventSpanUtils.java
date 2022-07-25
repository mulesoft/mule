/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenetelemetryCoreEventInternalSpanExporterFactory.getOpenetelemetryCoreEventInternalSpanExporterFactory;

import static java.util.Optional.empty;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpentelemetrySpanExporterManager;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.privileged.profiling.SpanExportManager;

import java.util.Optional;

/**
 * Utility class for Core Event Spans
 *
 * @since 4.5.0
 */
public class CoreEventSpanUtils {

  private static final String CORE_EVENT_SPAN_NAME_SEPARATOR = ":";
  public static final String UNKNOWN = "unknown";

  private CoreEventSpanUtils() {}

  /**
   * @param eventContext the {@link EventContext} to extract the parent span from.
   * @return the current context span
   *
   * @see CoreEventTracer
   */
  public static Optional<InternalSpan> getCurrentSpan(EventContext eventContext) {
    if (eventContext instanceof DistributedTraceContextAware) {
      return ((DistributedTraceContextAware) eventContext).getDistributedTraceContext().getCurrentSpan();
    }

    return empty();
  }

  /**
   * @param componentIdentifier the {@link ComponentIdentifier}
   * @return the name for the {@link org.mule.runtime.api.profiling.tracing.Span}
   */
  public static String getSpanName(ComponentIdentifier componentIdentifier) {
    return getUnknownIfEmptyNamespace(componentIdentifier) + CORE_EVENT_SPAN_NAME_SEPARATOR
        + getUnknownIfEmptyName(componentIdentifier);
  }

  private static String getUnknownIfEmptyName(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return UNKNOWN;
    }

    return componentIdentifier.getName();
  }

  private static String getUnknownIfEmptyNamespace(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return UNKNOWN;
    }

    return componentIdentifier.getNamespace();
  }

  /**
   * @return the default {@link InternalSpanExportManager}.
   */
  public static InternalSpanExportManager<EventContext> getDefaultSpanExporterManager() {
    return new OpentelemetrySpanExporterManager();
  }

}
