/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;

import static java.util.stream.Collectors.toList;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.routing.result.RoutingException;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Router that generates executable routes for each one configured and handles the logic of deciding which one will be executed
 * when an event arrives. It also provides access to the routes publishers so they can be merged together.
 */
abstract class AbstractSinkRouter {

  private final Flux<CoreEvent> router;
  private final List<ExecutableRoute> routes;
  private final ExecutableRoute phantomRoute;
  private final Scheduler subscriptionScheduler;

  protected AbstractSinkRouter(Publisher<CoreEvent> publisher, List<ProcessorRoute> routes,
                               ComponentTracerFactory componentTracerFactory, Scheduler subscriptionScheduler) {
    this.subscriptionScheduler = subscriptionScheduler;
    this.routes = routes.stream().map(ProcessorRoute::toExecutableRoute).collect(toList());

    // This phantomRoute exists so that the subscription/completion mechanism does not interfere with an actual route.
    this.phantomRoute = new ExecutableRoute(new ProcessorRoute(e -> e, componentTracerFactory));

    router = from(publisher)
        .doOnNext(checkedConsumer(this::route))
        .doOnComplete(() -> {
          this.routes.stream().forEach(ExecutableRoute::complete);
          phantomRoute.complete();
        }).doOnError(this.phantomRoute::error);
  }

  /**
   * @return the publishers of all routes so they can be merged and subscribed, including a phantom one to guarantee the
   *         subscription of the router occurs after all routes and with the general context.
   */
  List<Flux<CoreEvent>> collectPublishers() {
    List<Flux<CoreEvent>> routePublishers = new ArrayList<>();
    for (ExecutableRoute nextRoute : this.routes) {
      routePublishers.add(nextRoute.getPublisher());
    }

    routePublishers.add(subscribeFluxOnPublisherSubscription(phantomRoute.getPublisher(), router, subscriptionScheduler));

    return routePublishers;
  }

  /**
   * Decides which route should execute for an incoming event and executes it.
   *
   * @param event the incoming event
   */
  protected abstract void route(CoreEvent event) throws RoutingException;

  protected List<ExecutableRoute> getRoutes() {
    return routes;
  }

}
