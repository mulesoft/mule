/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor.w3c;

import org.mule.runtime.tracer.impl.context.extractor.TraceContextFieldExtractor;
import org.mule.runtime.tracer.impl.context.extractor.AbstractSingleTraceContextFieldExtractor;

/**
 * A {@link TraceContextFieldExtractor} that extracts the <a href="https://www.w3.org/TR/trace-context">tracestate header</a>.
 *
 * @since 4.5.0
 */
public class TraceStateContextFieldExtractor extends AbstractSingleTraceContextFieldExtractor {

  public static final String TRACESTATE = "tracestate";

  @Override
  public String getFieldKey() {
    return TRACESTATE;
  }
}

