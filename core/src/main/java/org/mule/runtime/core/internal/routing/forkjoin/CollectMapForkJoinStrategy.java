/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.forkjoin;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.message.Message.of;

import org.mule.runtime.core.api.Event;

import java.util.List;
import java.util.function.Function;

/**
 * {@link org.mule.runtime.core.api.routing.ForkJoinStrategy} that:
 * <ul>
 * <li>Performs parallel execution of route pairs subject to {@code maxConcurrency}.
 * <li>Merges variables using a last-wins strategy.
 * <li>Waits for the completion of all routes before emitting a result event, with an optional timeout.
 * <li>Collects results into a result {@link Event} with a {@link java.util.Map<String, org.mule.runtime.api.message.Message>}
 * payload where the {@link java.util.Map} key is a string representation of the sequence number of the
 * {@link org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair}.
 * <li>Will processor all routes, regardless of errors, and propagating a composite exception where there were one or more errors.
 * </ul>
 */
public class CollectMapForkJoinStrategy extends AbstractForkJoinStrategy {

  public CollectMapForkJoinStrategy() {
    super();
  }

  public CollectMapForkJoinStrategy(long timeout) {
    super(timeout);
  }

  @Override
  protected Function<List<Event>, Event> createResultEvent(Event original, Event.Builder resultBuilder) {
    return list -> resultBuilder
        .message(of(list.stream().collect(toMap(event -> Integer.toString(event.getGroupCorrelation().get().getSequence()),
                                                event -> event.getMessage()))))
        .build();
  }


}
