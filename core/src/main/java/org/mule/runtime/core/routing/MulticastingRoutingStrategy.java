/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Routing strategy that will route a message through a set of {@link Processor} and return an aggregation of the results.
 *
 */
public class MulticastingRoutingStrategy extends AbstractRoutingStrategy {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());
  private final RouterResultsHandler resultsHandler;

  /**
   * @param muleContext
   * @param resultAggregator aggregator used to create a response event
   */
  public MulticastingRoutingStrategy(MuleContext muleContext, RouterResultsHandler resultAggregator) {
    super(muleContext);
    this.resultsHandler = resultAggregator;
  }

  @Override
  public Event route(Event event, List<Processor> messageProcessors) throws MuleException {
    InternalMessage message = event.getMessage();

    if (messageProcessors == null || messageProcessors.size() == 0) {
      throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), null);
    }

    List<Event> results = new ArrayList<>(messageProcessors.size());

    validateMessageIsNotConsumable(event, message);

    try {
      for (int i = 0; i < messageProcessors.size(); i++) {
        Processor mp = messageProcessors.get(i);
        Event result = sendRequest(event, message, mp, true);
        if (result != null) {
          results.add(result);
        }
      }
    } catch (MuleException e) {
      throw new CouldNotRouteOutboundMessageException(messageProcessors.get(0), e);
    }
    return resultsHandler.aggregateResults(results, event);
  }



}
