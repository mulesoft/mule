/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import org.reactivestreams.Publisher;

/**
 * Strategy that defines how a set of {@link RoutingPair}'s each consisting of a {@link Processor} and {@link Event} will be
 * processed and a single result {@link Event} returned. This will normally be used for:
 * <ul>
 * <li>Routing {@code n} {@link Event}'s to the same {@link Processor}.
 * <li>Routing a single {@link Event} to {@code n} {@link Processor}'s.
 * </ul>
 * <p>
 * Implementations will typically implement parallel behavior where the invocation of each route is independent and then results
 * are aggregated, but other implements such as strict sequential invocation or even the use of a shared context between
 * invocations are possible.
 * <p>
 * While the result of this strategy is a single {@link Event} implementations are free to decide if the event should be emitted
 * only once all results are available, or if it emits the event immediately and then makes the results available via an
 * {@link java.util.Iterator} or {@link Publisher<Event>} payload. Implementations may also return the original {@link Event}
 * therefore performing a simple join with no aggregation.
 * 
 * @since 4.0
 */
public interface ForkJoinStrategy {

  Publisher<Event> forkJoin(Event original, Publisher<RoutingPair> routingPairs, ProcessingStrategy processingStrategy,
                            int maxConcurrency, boolean delayErrors);

  final class RoutingPair {

    private Processor processor;
    private Event event;

    public static RoutingPair of(Event event, Processor route) {
      return new RoutingPair(event, route);
    }

    private RoutingPair(Event event, Processor route) {
      this.event = event;
      this.processor = route;
    }

    public Processor getProcessor() {
      return processor;
    }

    public Event getEvent() {
      return event;
    }

  }

}
