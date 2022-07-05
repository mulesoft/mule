/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.visitor;

import static org.mule.runtime.core.internal.event.trace.EventDistributedTraceContext.builder;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.event.trace.DistributedTraceContextGetter;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

/**
 * A {@link EventContextVisitor} that sets the {@link DistributedTraceContext} if possible depending on the subtype of the
 * {@link EventContext} passed as parameter or else returns a default empty distributed trace context.
 *
 * @since 4.5.0
 */
public class SetIfPossibleDistributedTraceContextEventContextVisitor implements EventContextVisitorForEventContext {

  private final DistributedTraceContextGetter distributedTraceContextGetter;

  public SetIfPossibleDistributedTraceContextEventContextVisitor(DistributedTraceContextGetter distributedTraceContextGetter) {
    this.distributedTraceContextGetter = distributedTraceContextGetter;
  }

  @Override
  public EventContext visit(DefaultEventContext eventContext) {
    eventContext.setDistributedTraceContext(getDistributedTraceContext(distributedTraceContextGetter));
    return eventContext;
  }

  @Override
  public EventContext visit(EventContext eventContext) {
    return eventContext;
  }

  private DistributedTraceContext getDistributedTraceContext(DistributedTraceContextGetter distributedTraceContextGetter) {
    return builder().withGetter(distributedTraceContextGetter).build();
  }
}
