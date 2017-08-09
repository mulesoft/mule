/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

/**
 * Strategy that defines how a set of {@link RoutingPair}'s each consisting of a {@link Processor} and {@link InternalEvent} will
 * be processed and a single result {@link InternalEvent} returned. This will normally be used for:
 * <ul>
 * <li>Routing {@code n} {@link InternalEvent}'s to the same {@link MessageProcessorChain}.
 * <li>Routing a single {@link InternalEvent} to {@code n} {@link MessageProcessorChain}'s.
 * </ul>
 * <p>
 * Implementations will typically implement parallel behavior where the invocation of each route is independent and then results
 * are aggregated, but other implements such as strict sequential invocation or even the use of a shared context between
 * invocations are possible.
 * <p>
 * While the result of this strategy is a single {@link InternalEvent} implementations are free to decide if the event should be
 * emitted only once all results are available, or if it emits the event immediately and then makes the results available via an
 * {@link java.util.Iterator} or {@link Publisher< InternalEvent >} payload. Implementations may also return the original
 * {@link InternalEvent} therefore performing a simple join with no aggregation.
 * 
 * @since 4.0
 */
public interface ForkJoinStrategy {

  Publisher<InternalEvent> forkJoin(InternalEvent original, Publisher<RoutingPair> routingPairs);

  final class RoutingPair {

    private MessageProcessorChain processor;
    private InternalEvent event;

    public static RoutingPair of(InternalEvent event, MessageProcessorChain route) {
      return new RoutingPair(event, route);
    }

    private RoutingPair(InternalEvent event, MessageProcessorChain route) {
      this.event = event;
      this.processor = route;
    }

    public MessageProcessorChain getRoute() {
      return processor;
    }

    public InternalEvent getEvent() {
      return event;
    }

  }

}
