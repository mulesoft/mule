/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor.w3c;

import org.mule.runtime.core.internal.event.trace.extractor.AbstractSingleTraceContextFieldExtractor;

/**
 * A {@link org.mule.runtime.core.internal.event.trace.extractor.TraceContextFieldExtractor} that extracts the
 * <a href="https://www.w3.org/TR/trace-context">traceparent header</a>.
 *
 * @since 4.5.0
 */
public class TraceParentContextFieldExtractor extends AbstractSingleTraceContextFieldExtractor {

  public static final String TRACEPARENT = "traceparent";

  @Override
  public String getFieldKey() {
    return TRACEPARENT;
  }
}
