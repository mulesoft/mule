/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.routing.RouterResultsHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The default results handler for all outbound endpoint. Depending on the number of messages passed it the returning message will
 * be different. If the 'results' param is null or empty, null is returned. If the 'results' param contains a single
 * {@link org.mule.runtime.core.api.InternalMessage}, than that message is returned.
 * <p/>
 * Note that right now (as of Mule 2.0.1) this SPI is not pluggable and this implementation is the default and only
 * implementation.
 *
 * @see org.mule.runtime.core.api.InternalMessage
 * @see org.mule.runtime.core.DefaultMessageCollection
 */
public class DefaultRouterResultsHandler implements RouterResultsHandler {

  private boolean returnCollectionWithSingleResult = false;

  public DefaultRouterResultsHandler() {}

  /**
   * @param returnCollectionWithSingleResult if a MuleMessageCollection should be return despite there's only one result event
   */
  public DefaultRouterResultsHandler(boolean returnCollectionWithSingleResult) {
    this.returnCollectionWithSingleResult = returnCollectionWithSingleResult;
  }

  /**
   * Aggregates the events in the results list into one single {@link org.mule.runtime.core.api.Event} You should only use this
   * method when you're sure that all the events in the results list were generated by the same thread that's going to handle the
   * aggregated response
   *
   * @param results
   * @param previous
   * @return
   */
  @Override
  @SuppressWarnings(value = {"unchecked"})
  public Event aggregateResults(final List<Event> results,
                                final Event previous) {
    if (results == null) {
      return null;
    } else if (results.size() == 1) {
      Event event = results.get(0);
      if (event == null || event instanceof VoidMuleEvent) {
        return event;
      } else if (event != null && event.getMessage() != null) {
        if (returnCollectionWithSingleResult) {
          return createMessageCollectionWithSingleMessage(event);
        } else {
          return event;
        }
      } else {
        return VoidMuleEvent.getInstance();
      }
    } else {
      List<Event> nonNullResults = results.stream().filter(object -> {
        return !VoidMuleEvent.getInstance().equals(object) &&
            object != null &&
            object.getMessage() != null;
      }).collect(toList());

      if (nonNullResults.size() == 0) {
        return VoidMuleEvent.getInstance();
      } else if (nonNullResults.size() == 1) {
        return nonNullResults.get(0);
      } else {
        return createMessageCollection(nonNullResults, previous);
      }
    }
  }

  private Event createMessageCollectionWithSingleMessage(Event event) {
    final InternalMessage coll = InternalMessage.builder()
        .collectionPayload(singletonList(event.getMessage()), InternalMessage.class)
        .build();
    event = Event.builder(event).message(coll).build();
    setCurrentEvent(event);
    return event;
  }

  private Event createMessageCollection(final List<Event> nonNullResults,
                                        final Event previous) {
    final Builder resultBuilder = Event.builder(previous);

    List<InternalMessage> list = new ArrayList<>();
    for (Event event : nonNullResults) {
      for (String flowVarName : event.getVariableNames()) {
        resultBuilder.addVariable(flowVarName, event.getVariable(flowVarName).getValue(),
                                  event.getVariable(flowVarName).getDataType());
      }
      list.add(event.getMessage());
    }
    final InternalMessage coll = InternalMessage.builder()
        .collectionPayload(list, InternalMessage.class)
        .build();

    Event resultEvent = resultBuilder.message(coll).build();
    setCurrentEvent(resultEvent);
    return resultEvent;
  }
}
