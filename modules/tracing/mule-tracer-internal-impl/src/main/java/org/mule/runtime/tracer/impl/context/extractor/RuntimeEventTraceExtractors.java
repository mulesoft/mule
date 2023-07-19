/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor;

import org.mule.runtime.tracer.impl.context.extractor.w3c.TraceParentContextFieldExtractor;
import org.mule.runtime.tracer.impl.context.extractor.w3c.TraceStateContextFieldExtractor;

/**
 * Default {@link TraceContextFieldExtractor} used by the runtime to propagate a trace context.
 *
 * @since 4.5.0
 */
public class RuntimeEventTraceExtractors {

  private static final TraceContextFieldExtractor DEFAULT_TRACE_CONTEXT_FIELDS_EXTRACTOR =
      new ComposedTraceContextFieldExtractor(new CorrelationIdTraceContextFieldExtractor(),
                                             new TraceParentContextFieldExtractor());

  private static final TraceContextFieldExtractor DEFAULT_BAGGAGE_FIELDS_EXTRACTOR = new TraceStateContextFieldExtractor();

  private RuntimeEventTraceExtractors() {}

  /**
   * @return the default trace context field extractor.
   */
  public static TraceContextFieldExtractor getDefaultTraceContextFieldsExtractor() {
    return DEFAULT_TRACE_CONTEXT_FIELDS_EXTRACTOR;
  }

  /**
   * @return the default trace context baggage extractor.
   */
  public static TraceContextFieldExtractor getDefaultBaggageExtractor() {
    return DEFAULT_BAGGAGE_FIELDS_EXTRACTOR;
  }
}
