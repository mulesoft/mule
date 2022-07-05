/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.internal.event.trace.visitor.GetOrDefaultDistributedTraceContextEventContextVisitor.getOrDefaultDistributedTraceContextEventContextVisitorInstance;
import static org.mule.runtime.core.internal.event.trace.visitor.VisitableEventContext.visitableEventContextFrom;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

/**
 * {@link ArgumentResolver} that yields instances of {@link DistributedTraceContextManager}
 *
 * @since 4.5.0
 */
public class DistributedTraceContextManagerResolver implements ArgumentResolver<DistributedTraceContextManager> {

  @Override
  public DistributedTraceContextManager resolve(ExecutionContext executionContext) {
    return new PropagateAllDistributedTraceContextManager(getDistributedTraceContext(((ExecutionContextAdapter<?>) executionContext)
        .getEvent()));
  }

  private DistributedTraceContext getDistributedTraceContext(CoreEvent event) {
    return visitableEventContextFrom(event.getContext()).accept(getOrDefaultDistributedTraceContextEventContextVisitorInstance());
  }
}
