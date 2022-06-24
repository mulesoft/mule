/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor;

import org.mule.sdk.api.runtime.source.SdkDistributedTraceContextMapGetter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A composed {@link TraceContextFieldExtractor}. It extracts all the fields according to the passed extractors.
 *
 * @since 4.5.0
 */
public class ComposedTraceContextFieldExtractor implements TraceContextFieldExtractor {

  private final TraceContextFieldExtractor[] traceContextFieldExtractors;

  public ComposedTraceContextFieldExtractor(TraceContextFieldExtractor... traceContextFieldExtractors) {
    this.traceContextFieldExtractors = traceContextFieldExtractors;
  }

  @Override
  public Map<String, String> extract(SdkDistributedTraceContextMapGetter sdkDistributedTraceContextMapGetter) {
    Map<String, String> result = new HashMap<>();
    Arrays.stream(traceContextFieldExtractors).forEach(traceContextFieldExtractor -> result
        .putAll(traceContextFieldExtractor.extract(sdkDistributedTraceContextMapGetter)));
    return result;
  }
}
