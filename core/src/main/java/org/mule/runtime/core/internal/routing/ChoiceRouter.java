/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;
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
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.core.privileged.routing.RouterStatisticsRecorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Routes the event to a single<code>MessageProcessor</code> using an expression to evaluate the event being processed and find
 * the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default route will be used. Otherwise it continues the
 * execution through the next MP in the chain.
 */
public class ChoiceRouter extends AbstractComponent implements Router, RouterStatisticsRecorder, Lifecycle, MuleContextAware {

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final List<ProcessorRoute> routes = new ArrayList<>();

  private Processor defaultProcessor;
  private RouterStatistics routerStatistics;
  private MuleContext muleContext;
  private ExpressionManager expressionManager;

  public ChoiceRouter() {
    routerStatistics = new RouterStatistics(TYPE_OUTBOUND);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Inject
  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (defaultProcessor == null) {
      defaultProcessor = event -> event;
    }
    routes.add(new ProcessorRoute(defaultProcessor));

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

  public void addRoute(final String expression, final Processor processor) {
    routes.add(new ProcessorExpressionRoute(expression, processor));
  }

  public void setDefaultRoute(final Processor processor) {
    defaultProcessor = processor;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return Flux.merge(new SinkRouter(publisher, routes).collectPublishers());
  }

  public void updateStatistics(Processor processor) {
    if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
      getRouterStatistics().incrementRoutedMessage(processor);
    }
  }

  public RouterStatistics getRouterStatistics() {
    return routerStatistics;
  }

  @Override
  public void setRouterStatistics(RouterStatistics routerStatistics) {
    this.routerStatistics = routerStatistics;
  }

  @Override
  public String toString() {
    return format("%s [flow=%s, started=%s]", getClass().getSimpleName(), getLocation().getRootContainerName(), started);
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
     */
    private void route(CoreEvent event) {
      ExecutableRoute selectedRoute;
      try (ExpressionManagerSession session = expressionManager.openSession(getLocation(), event, NULL_BINDING_CONTEXT)) {
        selectedRoute = routes.stream().filter(route -> route.shouldExecute(session)).findFirst().get();
      }
      selectedRoute.execute(event);
      updateStatistics(selectedRoute.getProcessor());
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
}
