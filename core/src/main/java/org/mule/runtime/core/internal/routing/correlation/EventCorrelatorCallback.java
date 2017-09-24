/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.core.internal.routing.AggregationException;
import org.mule.runtime.core.internal.routing.EventGroup;

/**
 * A callback used to allow pluggable behaviour when correlating events
 */
public interface EventCorrelatorCallback {

  /**
   * Determines if the event group is ready to be aggregated. if the group is ready to be aggregated (this is entirely up to the
   * application. it could be determined by volume, last modified time or some oher criteria based on the last event received).
   *
   * @param events The current event group received by the correlator
   * @return true if the group is ready for aggregation
   */
  public boolean shouldAggregateEvents(EventGroup events);

  /**
   * This method is invoked if the shouldAggregate method is called and returns true. Once this method returns an aggregated
   * message, the event group is removed from the router.
   *
   * @param events the event group for this request
   * @return an aggregated message
   * @throws AggregationException if the aggregation fails. in this scenario the whole event group is removed and passed to the
   *         exception handler for this component
   */
  public CoreEvent aggregateEvents(EventGroup events) throws RoutingException;


  /**
   * Creates the event group with a specific correlation size based on the Mule GroupCorrelation support
   *
   * @param id The group id
   * @param event the current event
   * @return a new event group of a fixed size
   */
  public EventGroup createEventGroup(CoreEvent event, Object id);
}
