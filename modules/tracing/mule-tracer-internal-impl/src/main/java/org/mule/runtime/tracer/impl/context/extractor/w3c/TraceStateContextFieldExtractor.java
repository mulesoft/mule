/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

