/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import org.mule.runtime.api.event.EventContext;

import java.util.Map;

/**
 * A method to retrieve a trace context from a carrier.
 *
 * @param <T> the type of the carrier.
 */
public interface GetTraceContextMapMethod<T extends EventContext> {

  /**
   * @param carrier The event context to retrieve the distributed trace context from.
   * @return a map containing the span context to propagate.
   */
  Map<String, String> getDistributedTraceContextMap(T carrier);

}
