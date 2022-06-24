/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor;

import org.mule.runtime.core.internal.event.trace.extractor.w3c.TraceParentContextFieldExtractor;
import org.mule.runtime.core.internal.event.trace.extractor.w3c.TraceStateContextFieldExtractor;

/**
 * Default {@link TraceContextFieldExtractor} used by the runtime to propagate a trace context.
 *
 * @since 4.5.0
 */
public class RuntimeEventTraceExtractors {

  /**
   * @return the default trace context field extractor.
   */
  public static TraceContextFieldExtractor getDefaultTraceContextFieldsExtractor() {
    return new ComposedTraceContextFieldExtractor(new CorrelationIdTraceContextFieldExtractor(),
                                                  new TraceParentContextFieldExtractor());
  }

  /**
   * @return the default trace context baggage extractor.
   */
  public static TraceContextFieldExtractor getDefaultBaggageExtractor() {
    return new TraceStateContextFieldExtractor();
  }
}
