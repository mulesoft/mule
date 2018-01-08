/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import static java.util.Optional.empty;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.routing.AggregationException;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Optional;

/**
 * A Correlator that correlates messages based on Mule correlation settings
 */
public class CollectionCorrelatorCallback implements EventCorrelatorCallback {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  protected MuleContext muleContext;
  private final String storePrefix;

  public CollectionCorrelatorCallback(MuleContext muleContext, String storePrefix) {
    this.muleContext = muleContext;
    this.storePrefix = storePrefix;
  }

  /**
   * This method is invoked if the shouldAggregate method is called and returns true. Once this method returns an aggregated
   * message, the event group is removed from the router.
   *
   * @param events the event group for this request
   * @return an aggregated message
   * @throws AggregationException if the aggregation fails. in this scenario the whole event group is removed and passed to the
   *         exception handler for this component
   */
  @Override
  public CoreEvent aggregateEvents(EventGroup events) throws AggregationException {
    return events.getMessageCollectionEvent();
  }

  protected MuleSession getMergedSession(PrivilegedEvent[] events) {
    MuleSession session = new DefaultMuleSession(events[0].getSession());
    for (int i = 1; i < events.length; i++) {
      for (String name : events[i].getSession().getPropertyNamesAsSet()) {
        session.setProperty(name, events[i].getSession().getProperty(name));
      }
    }
    return session;
  }

  /**
   * Creates a new EventGroup that will expect the number of events as returned by {@link org.mule.runtime.core.api.message.GroupCorrelation#getGroupSize()}.
   */
  @Override
  public EventGroup createEventGroup(CoreEvent event, Object groupId) {
    return new EventGroup(groupId, muleContext,
                          event.getGroupCorrelation().isPresent() ? event.getGroupCorrelation().get().getGroupSize().isPresent()
                              ? Optional.of(event.getGroupCorrelation().get().getGroupSize().getAsInt())
                              : empty() : Optional.empty(),
                          storePrefix);
  }

  /**
   * @return <code>true</code> if the correlation size is not set or exactly the expected size of the event group.
   * @see EventCorrelatorCallback#shouldAggregateEvents(EventGroup)
   */
  @Override
  public boolean shouldAggregateEvents(EventGroup events) {

    if (!events.expectedSize().isPresent()) {
      logger.warn("GroupCorrelation Group Size not set, but correlation aggregator is being used."
          + " Message is being forwarded as is");
      return true;
    }

    Integer size = events.expectedSize().get();
    if (logger.isDebugEnabled()) {
      logger.debug(MessageFormat.format("GroupCorrelation group size is {0}. Current event group size is {1} for group ID: {2}",
                                        size,
                                        events.size(), events.getGroupId()));
    }

    return size == events.size();
  }
}
