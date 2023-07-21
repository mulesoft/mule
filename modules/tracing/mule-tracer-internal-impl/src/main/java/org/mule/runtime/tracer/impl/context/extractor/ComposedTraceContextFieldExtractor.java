/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor;

import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;

import java.util.HashMap;
import java.util.Map;

/**
 * A composed {@link TraceContextFieldExtractor}. It extracts all the fields extracted by the extractors passed as parameters.
 *
 * @since 4.5.0
 */
public class ComposedTraceContextFieldExtractor implements TraceContextFieldExtractor {

  private final TraceContextFieldExtractor[] traceContextFieldExtractors;

  public ComposedTraceContextFieldExtractor(TraceContextFieldExtractor... traceContextFieldExtractors) {
    this.traceContextFieldExtractors = traceContextFieldExtractors;
  }

  @Override
  public Map<String, String> extract(DistributedTraceContextGetter distributedTraceContextMapGetter) {
    Map<String, String> result = new HashMap<>();

    for (TraceContextFieldExtractor traceContextFieldExtractor : traceContextFieldExtractors) {
      result.putAll(traceContextFieldExtractor.extract(distributedTraceContextMapGetter));
    }

    return result;
  }
}
