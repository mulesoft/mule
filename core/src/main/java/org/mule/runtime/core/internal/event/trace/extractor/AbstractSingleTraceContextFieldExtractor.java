/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor;

import org.mule.sdk.api.runtime.source.SdkDistributedTraceContextMapGetter;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TraceContextFieldExtractor} for a single field.
 *
 * @since 4.5.0
 */
public abstract class AbstractSingleTraceContextFieldExtractor implements TraceContextFieldExtractor {

  @Override
  public Map<String, String> extract(SdkDistributedTraceContextMapGetter sdkDistributedTraceContextMapGetter) {
    Map<String, String> resultContext = new HashMap<>();
    sdkDistributedTraceContextMapGetter.get(getFieldKey()).ifPresent(value -> resultContext.put(getFieldKey(), value));

    return resultContext;
  }

  /**
   * @return the key for the field to extract.
   */
  public abstract String getFieldKey();
}
