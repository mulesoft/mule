/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.routing.AggregationContext;
import org.mule.runtime.core.api.routing.AggregationStrategy;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.privileged.routing.DefaultRouterResultsHandler;

/**
 * If no routes generated exeption then it returns a new {@link Event} under the rules of {@link DefaultRouterResultsHandler} (you
 * can change this behaviour by overriding {@link #aggregateWithoutFailedRoutes(AggregationContext)}. Otherwise, a
 * {@link CompositeRoutingException} is thrown (override {@link #aggregateWithFailedRoutes(AggregationContext) to customize}
 * 
 * @since 3.5.0
 */
public class CollectAllAggregationStrategy implements AggregationStrategy {

  private RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

  @Override
  public Event aggregate(AggregationContext context) throws MuleException {
    if (context.collectEventsWithExceptions().isEmpty()) {
      return this.aggregateWithoutFailedRoutes(context);
    } else {
      return this.aggregateWithFailedRoutes(context);
    }
  }

  protected Event aggregateWithoutFailedRoutes(AggregationContext context) throws MuleException {
    return this.resultsHandler.aggregateResults(context.getEvents(), context.getOriginalEvent());
  }

  protected Event aggregateWithFailedRoutes(AggregationContext context) throws MuleException {
    throw new CompositeRoutingException(context.collectRouteExceptions());
  }

}
