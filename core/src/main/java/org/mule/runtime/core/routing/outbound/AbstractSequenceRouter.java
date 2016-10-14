/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transport.LegacyOutboundEndpoint;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.runtime.core.routing.AbstractRoutingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a router that sequentially routes a given message to the list of registered endpoints and returns the aggregate
 * responses as the result. Aggregate response is built using the partial responses obtained from synchronous endpoints. The
 * routing process can be stopped after receiving a partial response.
 */
public abstract class AbstractSequenceRouter extends FilteringOutboundRouter {

  @Override
  public Event route(Event event) throws RoutingException {
    InternalMessage message = event.getMessage();

    if (routes == null || routes.size() == 0) {
      throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), null);
    }

    Builder builder = Event.builder(event).groupCorrelation(new GroupCorrelation(routes.size(), null));
    List<Event> results = new ArrayList<>(routes.size());
    try {
      for (int i = 0; i < routes.size(); i++) {
        Processor mp = getRoute(i, event);

        boolean filterAccepted =
            !(mp instanceof LegacyOutboundEndpoint) || ((LegacyOutboundEndpoint) mp).filterAccepts(message, builder);
        event = builder.build();
        builder = Event.builder(event);
        if (filterAccepted) {
          AbstractRoutingStrategy.validateMessageIsNotConsumable(event, message);
          InternalMessage clonedMessage = cloneMessage(event, message);

          Event result = sendRequest(event, createEventToRoute(event, clonedMessage), mp, true);
          if (result != null) {
            results.add(result);
          }

          if (!continueRoutingMessageAfter(result)) {
            break;
          }
        }
      }
    } catch (MuleException e) {
      throw new CouldNotRouteOutboundMessageException(routes.get(0), e);
    }
    return resultsHandler.aggregateResults(results, builder.build());
  }


  /**
   * Lets subclasses decide if the routing of a given message should continue or not after receiving a given response from a
   * synchronous endpoint.
   *
   * @param response the last received response
   * @return true if must continue and false otherwise.
   * @throws MuleException when the router should stop processing throwing an exception without returning any results to the
   *         caller.
   */
  protected abstract boolean continueRoutingMessageAfter(Event response) throws MuleException;
}
