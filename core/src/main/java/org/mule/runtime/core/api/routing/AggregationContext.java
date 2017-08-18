/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.api.util.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * Immutable object used to provide all the necessary information to perform an aggregation operation in one single parameter,
 * helping to maintain consistent and simple signatures across aggregators The most important attribute in this class is
 * {@link #events} which holds the events to be aggregated. These events need to be ordered so that each event's index corresponds
 * to the index of each route
 * 
 * @since 3.5.0
 */
public final class AggregationContext {

  private static final Predicate failedEventsPredicate = new Predicate() {

    @Override
    public boolean evaluate(Object object) {
      if (object == null) {
        return false;
      }

      InternalEvent event = (InternalEvent) object;
      return event.getError().isPresent();
    }
  };

  /**
   * the original event from wich the events to be aggregated were splitted from
   */
  private final InternalEvent originalEvent;

  /**
   * The events to be aggregated. These events need to be ordered so that each event's index corresponds to the index of each
   * route
   */
  private final List<InternalEvent> events;

  /**
   * Creates a new instance
   * 
   * @param originalEvent a {@link InternalEvent}. Can be <code>null</code>
   * @param events a {@link List} of {@link InternalEvent}. Cannot be <code>null</code> but could be empty. In that case, is up to each
   *        consumer to decide wether to fail or not
   */
  public AggregationContext(InternalEvent originalEvent, List<InternalEvent> events) {
    Preconditions.checkArgument(events != null, "events cannot be null");
    this.originalEvent = originalEvent;
    this.events = Collections.unmodifiableList(events);
  }

  /**
   * Returns all the {@link InternalEvent}s which messages have a not <code>null</code> {@link ExceptionPayload} and a not
   * <code>null</code> {@link ExceptionPayload#getException()}. Notice that this is a select operation. Each time this method is
   * invoked the result will be re-calculated
   * 
   * @return a list of {@link InternalEvent}. It could be empty but it will never be <code>null</code>
   */
  @SuppressWarnings("unchecked")
  public List<InternalEvent> collectEventsWithExceptions() {
    return (List<InternalEvent>) CollectionUtils.select(this.events, failedEventsPredicate);
  }

  /**
   * Returns a {@link NavigableMap} in which the key is a zero-based route index and the value is an {@link Throwable} generated
   * by it. Notice that this is a collect operation. Each time this method is invoked the result will be re-calculated
   * 
   * @return a @{link {@link NavigableMap}. It could be empty but it will never be <code>null</code>
   */
  public NavigableMap<String, Throwable> collectRouteExceptions() {
    NavigableMap<String, Throwable> routes = new TreeMap<>();
    for (int i = 0; i < this.events.size(); i++) {
      InternalEvent event = this.events.get(i);
      if (failedEventsPredicate.evaluate(event)) {
        routes.put(Integer.toString(i), event.getError().get().getCause());
      }
    }

    return routes;
  }

  /**
   * The exact opposite to {@link #collectEventsWithExceptions()} Returns all the {@link InternalEvent}s which messages have a
   * <code>null</code> {@link ExceptionPayload} or a <code>null</code> {@link ExceptionPayload#getException()}. Notice that this
   * is a collect operation. Each time this method is invoked the result will be re-calculated
   * 
   * @return a list of {@link InternalEvent}. It could be empty but it will never be <code>null</code>
   */
  @SuppressWarnings("unchecked")
  public List<InternalEvent> collectEventsWithoutExceptions() {
    return (List<InternalEvent>) CollectionUtils.selectRejected(this.events, failedEventsPredicate);
  }

  public InternalEvent getOriginalEvent() {
    return this.originalEvent;
  }

  public List<InternalEvent> getEvents() {
    return this.events;
  }

}
