/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor;

import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TraceContextFieldExtractor} for a single field.
 *
 * @since 4.5.0
 */
public abstract class AbstractSingleTraceContextFieldExtractor implements TraceContextFieldExtractor {

  @Override
  public Map<String, String> extract(DistributedTraceContextGetter distributedTraceContextMapGetter) {
    Map<String, String> resultContext = new HashMap<>();
    distributedTraceContextMapGetter.get(getFieldKey()).ifPresent(value -> resultContext.put(getFieldKey(), value));

    return resultContext;
  }

  /**
   * @return the key for the field to extract.
   */
  public abstract String getFieldKey();
}
