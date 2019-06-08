/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Composition of a {@link ProcessorRoute} and everything required to convert it into a reactor executable chain.
 */
public class ExecutableRoute {

  private ProcessorRoute route;
  private Flux<CoreEvent> publisher;
  private LazyValue<FluxSink<CoreEvent>> sink;

  ExecutableRoute(ProcessorRoute route) {
    this.route = route;
    FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();
    publisher = Flux.create(sinkRef).transform(route.getProcessor());
    sink = new LazyValue<>(() -> sinkRef.getFluxSink());
  }

  /**
   * Analyzes whether this route should execute, allowing the to separate the check from the execution.
   *
   * @param session an {@link ExpressionManagerSession}
   * @return {@code true} if this route should execute
   */
  boolean shouldExecute(ExpressionManagerSession session) {
    return route.accepts(session);
  }

  /**
   * Routes the incoming event through the route's sink.
   *
   * @param event the {@link CoreEvent} to process
   */
  void execute(CoreEvent event) {
    sink.get().next(event);
  }

  public Flux<CoreEvent> getPublisher() {
    return publisher;
  }

  public Processor getProcessor() {
    return route.getProcessor();
  }

}
