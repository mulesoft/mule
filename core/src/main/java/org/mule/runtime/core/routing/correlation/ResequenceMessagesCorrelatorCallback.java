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
 * A Correlator that correlates messages based on Mule correlation settings Note that the
 * {@link #aggregateEvents(org.mule.runtime.core.routing.EventGroup)} method only resequences the events and returns an
 * MuleEvent[] wrapped in a MuleMessage impl. This means that this callback can ONLY be used with a {@link Resequencer}
 */
public class ResequenceMessagesCorrelatorCallback extends CollectionCorrelatorCallback {

  protected Comparator eventComparator;
  protected MuleContext muleContext;

  public ResequenceMessagesCorrelatorCallback(Comparator eventComparator, MuleContext muleContext, String storePrefix) {
    super(muleContext, storePrefix);
    this.eventComparator = eventComparator;
    this.muleContext = muleContext;
  }

  /**
   * This method is invoked if the shouldAggregate method is called and returns true. Once this method returns an aggregated
   * message, the event group is removed from the router.
   * 
   * @param events the event group for this request
   * @return an aggregated message
   * @throws org.mule.runtime.core.routing.AggregationException if the aggregation fails. in this scenario the whole event group
   *         is removed and passed to the exception handler for this componenet
   */
  @Override
  public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
    MuleEvent[] results;
    try {
      results = (events == null) ? new MuleEvent[0] : events.toArray(false);
    } catch (ObjectStoreException e) {
      throw new AggregationException(events, null, e);
    }
    Arrays.sort(results, eventComparator);
    // This is a bit of a hack since we return a collection of events on one
    // message
    for (int i = 0; i < results.length; i++) {
      results[i] = MuleEvent.builder(results[i]).build();
    }
    return MuleEvent.builder(results[0]).message(MuleMessage.builder().payload(results).build()).build();
  }

}
