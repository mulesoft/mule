/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.extension.api.runtime.parameter.DistributedTraceContextPropagator;
import org.mule.runtime.extension.api.runtime.parameter.DistributedTraceContextSetter;

/**
 * A {@link DistributedTraceContextPropagator} that injects all the fields of the propagator context of the event.
 *
 * @since 4.5.0
 */
public class PropagateAllDistributedTraceContextPropagator implements DistributedTraceContextPropagator {

  private final DistributedTraceContext distributedTraceContext;

  public PropagateAllDistributedTraceContextPropagator(DistributedTraceContext distributedTraceContext) {
    this.distributedTraceContext = distributedTraceContext;
  }

  @Override
  public void injectDistributedTraceFields(DistributedTraceContextSetter setter) {
    // By default this will propagate all the fields in the distributed trace context.
    distributedTraceContext.tracingFieldsAsMap().forEach(setter::set);
    distributedTraceContext.baggageItemsAsMap().forEach(setter::set);
  }
}
