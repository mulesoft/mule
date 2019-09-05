/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.routing.RoutingException;

import java.util.ArrayList;
import java.util.Iterator;
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

  protected AbstractSinkRouter(Publisher<CoreEvent> publisher, List<ProcessorRoute> routes) {
    this.routes = routes.stream().map(ProcessorRoute::toExecutableRoute).collect(toList());
    router = from(publisher)
        .doOnNext(checkedConsumer(this::route))
        .doOnComplete(() -> this.routes.stream()
            .forEach(executableRoute -> executableRoute.complete()));
  }

  /**
   * @return the publishers of all routes so they can be merged and subscribed, including a phantom one to guarantee the
   *         subscription of the router occurs after all routes and with the general context.
   */
  List<Flux<CoreEvent>> collectPublishers() {
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
