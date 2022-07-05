/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;

import org.mule.runtime.core.internal.event.trace.DistributedTraceContextGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TraceContextFieldExtractor} for correlation id fields.
 *
 * @since 4.5.0
 */
public class CorrelationIdTraceContextFieldExtractor implements TraceContextFieldExtractor {

  public static final String X_CORRELATION_ID = "X-Correlation-ID";
  public static final String MULE_CORRELATION_ID = MULE_CORRELATION_ID_PROPERTY.toLowerCase();

  @Override
  public Map<String, String> extract(DistributedTraceContextGetter sdkDistributedTraceContextMapGetter) {
    Map<String, String> resultContext = new HashMap<>();
    Optional<String> xCorrelationId = sdkDistributedTraceContextMapGetter.get(X_CORRELATION_ID);
    Optional<String> muleCorrelationId = sdkDistributedTraceContextMapGetter.get(MULE_CORRELATION_ID);

    if (xCorrelationId.isPresent()) {
      resultContext.put(X_CORRELATION_ID, xCorrelationId.get());
    } else {
      muleCorrelationId.ifPresent(s -> resultContext.put(MULE_CORRELATION_ID, s));
    }

    return resultContext;
  }
}
