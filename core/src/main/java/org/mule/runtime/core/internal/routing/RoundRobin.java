/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.core.privileged.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.privileged.routing.RoutingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * RoundRobin divides the messages it receives among its target routes in round-robin fashion. This includes messages received on
 * all threads, so there is no guarantee that messages received from a splitter are sent to consecutively numbered targets.
 */
public class RoundRobin extends AbstractComponent implements Router, Lifecycle, MuleContextAware {

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final List<ProcessorRoute> routes = new ArrayList<>();

  /** Index of target route to use */
  private final AtomicInteger index = new AtomicInteger(0);

  private MuleContext muleContext;

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
    routes.add(new ProcessorRoute(processor));
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
   * Router that generates executable routes for each one configured and handles the logic of deciding which one will be executed
   * when an event arrives. It also provides access to the routes publishers so they can be merged together.
   */
  private class SinkRouter {

    private final Flux<CoreEvent> router;
    private final List<ExecutableRoute> routes;

    SinkRouter(Publisher<CoreEvent> publisher, List<ProcessorRoute> routes) {
      this.routes = routes.stream().map(ProcessorRoute::toExecutableRoute).collect(toList());
      router = from(publisher)
          .doOnNext(checkedConsumer(this::route))
          .doOnComplete(() -> this.routes.stream()
              .forEach(executableRoute -> executableRoute.complete()));
    }

    /**
     * Decides which route should execute for an incoming event and executes it, purposely separating those actions so that a
     * single {@link ExpressionManagerSession} can be used for all routes decision process.
     *
     * @param event the incoming event
     * @throws RoutingException
     */
    private void route(CoreEvent event) throws RoutingException {
      int modulo = getAndIncrementModuloN(routes.size());
      if (modulo < 0) {
        throw new CouldNotRouteOutboundMessageException(RoundRobin.this);
      }

      routes.get(modulo).execute(event);
    }

    /**
     * @return the publishers of all routes so they can be merged and subscribed, including a phantom one to guarantee the
     *         subscription of the router occurs after all routes and with the general context.
     */
    private List<Flux<CoreEvent>> collectPublishers() {
      List<Flux<CoreEvent>> routes = new ArrayList<>();
      for (Iterator routesIterator = this.routes.iterator(); routesIterator.hasNext();) {
        ExecutableRoute nextRoute = (ExecutableRoute) routesIterator.next();
        if (routesIterator.hasNext()) {
          routes.add(nextRoute.getPublisher());
        } else {
          // If it's the last route, this will be trigger for the whole inbound chain of the router to be subscribed.
          // Since there's always at least one route, the default one, one route will always be decorated.
          routes.add(subscribeFluxOnPublisherSubscription(nextRoute.getPublisher(), router));
        }
      }
      return routes;
    }
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
}
