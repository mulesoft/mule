/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.result.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.internal.routing.result.RoutingException;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * RoundRobin divides the messages it receives among its target routes in round-robin fashion. This includes messages received on
 * all threads, so there is no guarantee that messages received from a splitter are sent to consecutively numbered targets.
 */
public class RoundRobin extends AbstractComponent implements Router, Lifecycle, MuleContextAware {

  public static final String ROUND_ROBIN_ROUTE_SPAN_NAME_SUFFIX = ":route";
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final List<ProcessorRoute> routes = new ArrayList<>();

  /** Index of target route to use */
  private final AtomicInteger index = new AtomicInteger(0);

  private MuleContext muleContext;

  @Inject
  ComponentTracerFactory componentTracerFactory;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public void setRoutes(Collection<Processor> routes) {
    routes.forEach(this::addRoute);
  }

  @Override
  public void initialise() throws InitialisationException {
    for (ProcessorRoute route : routes) {
      route.setComponentTracer(componentTracerFactory.fromComponent(this, ROUND_ROBIN_ROUTE_SPAN_NAME_SUFFIX));
      initialiseIfNeeded(route, muleContext);
    }
  }

  @Override
  public void start() throws MuleException {
    for (ProcessorRoute route : routes) {
      route.start();
    }

    started.set(true);
  }

  @Override
  public void stop() throws MuleException {
    for (ProcessorRoute route : routes) {
      route.stop();
    }

    started.set(false);
  }

  @Override
  public void dispose() {
    for (ProcessorRoute route : routes) {
      route.dispose();
    }
  }

  public void addRoute(final Processor processor) {
    routes.add(new ProcessorRoute(processor, componentTracerFactory));
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return Flux.merge(new SinkRouter(publisher, routes).collectPublishers());
  }

  /**
   * Get the index of the processor to use
   */
  private int getAndIncrementModuloN(int modulus) {
    if (modulus == 0) {
      return -1;
    }

    return index.getAndUpdate(lastIndex -> (lastIndex + 1) % modulus);
  }

  private class SinkRouter extends AbstractSinkRouter {

    SinkRouter(Publisher<CoreEvent> publisher, List<ProcessorRoute> routes) {
      super(publisher, routes, componentTracerFactory);
    }

    @Override
    protected void route(CoreEvent event) throws RoutingException {
      int modulo = getAndIncrementModuloN(routes.size());
      if (modulo < 0) {
        throw new CouldNotRouteOutboundMessageException(RoundRobin.this);
      }

      getRoutes().get(modulo).execute(event);
    }
  }
}
