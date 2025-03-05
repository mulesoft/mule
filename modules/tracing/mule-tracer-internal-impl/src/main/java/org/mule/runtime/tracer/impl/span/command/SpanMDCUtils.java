/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import org.slf4j.MDC;

/**
 * Utils for managing span information in the log4j MDC
 *
 * @since 4.5.0
 */
public class SpanMDCUtils {

  public static final String SPAN_ID_MDC_KEY = "span_id";
  public static final String TRACE_ID_MDC_KEY = "trace_id";
  public static final String TRACE_FLAGS_MDC_KEY = "trace_flags";
  private static final String TRACE_FLAGS_DEFAULT_HEX_VALUE = "01";

  private SpanMDCUtils() {

  }

  /**
   * Sets the span information to the log4j MDC
   *
   * @param span the span to set the information from.
   */
  public static void setCurrentTracingInformationToMdc(Span span) {
    SpanIdentifier spanIdentifier = span.getIdentifier();
    if (spanIdentifier != null && spanIdentifier.isValid()) {
      MDC.put(SPAN_ID_MDC_KEY, spanIdentifier.getId());
      MDC.put(TRACE_ID_MDC_KEY, spanIdentifier.getTraceId());
      MDC.put(TRACE_FLAGS_MDC_KEY, TRACE_FLAGS_DEFAULT_HEX_VALUE);
    }
  }

  /**
   * Removes the current span information from the MDC.
   */
  public static void removeCurrentTracingInformationFromMdc() {
    MDC.remove(SPAN_ID_MDC_KEY);
    MDC.remove(TRACE_ID_MDC_KEY);
  }
}
