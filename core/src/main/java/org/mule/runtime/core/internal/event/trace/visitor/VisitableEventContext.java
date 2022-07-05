/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.visitor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.time.Instant;

/**
 * A delegate for {@link EventContext} for implementing the visitor pattern.
 *
 * @since 4.5.0
 */
public class VisitableEventContext implements EventContext {

  private final EventContext delegate;

  public static VisitableEventContext visitableEventContextFrom(EventContext eventContext) {
    return new VisitableEventContext(eventContext);
  }

  private VisitableEventContext(EventContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public String getRootId() {
    return delegate.getRootId();
  }

  @Override
  public String getCorrelationId() {
    return delegate.getCorrelationId();
  }

  @Override
  public Instant getReceivedTime() {
    return delegate.getReceivedTime();
  }

  @Override
  public ComponentLocation getOriginatingLocation() {
    return delegate.getOriginatingLocation();
  }

  /**
   * @param eventContextVisitor the visitor to use.
   * @return the {@link DistributedTraceContext} resulting of visiting the {@link EventContextVisitor}
   */
  public DistributedTraceContext accept(EventContextVisitorForDistributedEventContext eventContextVisitor) {
    return eventContextVisitor.visit(this.getDelegate());
  }

  /**
   * @param eventContextVisitor the visitor to use.
   * @return the {@link EventContext} resulting of visiting the {@link EventContextVisitor}
   */
  public EventContext accept(EventContextVisitorForEventContext eventContextVisitor) {
    return eventContextVisitor.visit(this.getDelegate());
  }

  public EventContext getDelegate() {
    return delegate;
  }
}
