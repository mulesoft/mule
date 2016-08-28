/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.correlation;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.routing.AggregationException;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.routing.Resequencer;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A Correlator that correlates messages based on Mule correlation settings. Note that the
 * {@link #aggregateEvents(org.mule.runtime.core.routing.EventGroup)} method only resequences the events and returns an
 * MuleEvent[] wrapped in a MuleMessage impl. This means that this callback can ONLY be used with a {@link Resequencer}
 */
public class ResequenceCorrelatorCallback extends CollectionCorrelatorCallback {

  protected Comparator<MuleEvent> eventComparator;

  public ResequenceCorrelatorCallback(Comparator<MuleEvent> eventComparator, MuleContext muleContext, String storePrefix) {
    super(muleContext, storePrefix);
    this.eventComparator = eventComparator;
  }

  /**
   * This method is invoked if the shouldAggregate method is called and returns true. Once this method returns an aggregated
   * message, the event group is removed from the router.
   * 
   * @param events the event group for this request
   * @return an aggregated message
   * @throws AggregationException if the aggregation fails. in this scenario the whole event group is removed and passed to the
   *         exception handler for this componenet
   */
  @Override
  public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
    MuleEvent results[];

    try {
      results = events.toArray(false);
    } catch (ObjectStoreException e) {
      throw new AggregationException(events, null, e);
    }

    Arrays.sort(results, eventComparator);
    // This is a bit of a hack since we wrap the the collection of events in a
    // Mule Message to pass back
    return MuleEvent.builder(results[0].getParent()).message(MuleMessage.builder().payload(results).build()).build();
  }

}
