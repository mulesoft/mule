/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.parameter.DistributedTraceContextPropagator;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextPropagator;

/**
 * {@link ArgumentResolver} that yields instances of {@link DistributedTraceContextPropagator}
 *
 * @since 4.1
 */
public class DistributedTraceContextPropagatorResolver implements ArgumentResolver<DistributedTraceContextPropagator> {

  @Override
  public DistributedTraceContextPropagator resolve(ExecutionContext executionContext) {
    CoreEvent event = ((ExecutionContextAdapter) executionContext).getEvent();
    return new PropagateAllDistributedTraceContextPropagator(event.getContext().getDistributedTraceContext());
  }
}
