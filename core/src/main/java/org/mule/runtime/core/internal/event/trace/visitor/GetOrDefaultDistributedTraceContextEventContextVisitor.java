/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.visitor;

import static org.mule.runtime.core.internal.trace.DistributedTraceContext.emptyDistributedEventContext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

/**
 * A {@link EventContextVisitor} that retrieves the {@link DistributedTraceContext} if possible depending on the type of the
 * subtype of {@link EventContext} passed as parameter or else returns a default empty distributed trace context.
 *
 * @since 4.5.0
 */
public class GetOrDefaultDistributedTraceContextEventContextVisitor implements EventContextVisitorForDistributedEventContext {

  private static final EventContextVisitorForDistributedEventContext INSTANCE =
      new GetOrDefaultDistributedTraceContextEventContextVisitor();

  private GetOrDefaultDistributedTraceContextEventContextVisitor() {}

  public static EventContextVisitorForDistributedEventContext getOrDefaultDistributedTraceContextEventContextVisitorInstance() {
    return INSTANCE;
  }

  @Override
  public DistributedTraceContext visit(DefaultEventContext eventContext) {
    return eventContext.getDistributedTraceContext();
  }

  @Override
  public DistributedTraceContext visit(EventContext eventContext) {
    return emptyDistributedEventContext();
  }
}
