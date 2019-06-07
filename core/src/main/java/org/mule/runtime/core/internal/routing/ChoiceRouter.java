/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections.ListUtils.union;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RouterStatisticsRecorder;

import java.util.ArrayList;
import java.util.Collection;
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
public class ChoiceRouter extends AbstractComponent implements SelectiveRouter, RouterStatisticsRecorder, Lifecycle,
    MuleContextAware {

  private final AtomicBoolean initialised = new AtomicBoolean(false);
  private final AtomicBoolean starting = new AtomicBoolean(false);
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final List<ProcessorExpressionRoute> routes = new ArrayList<>();

  private Processor defaultProcessor;
  private ProcessorRoute defaultRoute;
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
    defaultRoute = new ProcessorRoute(defaultProcessor);

    synchronized (routes) {
      for (Object o : getLifecycleManagedObjects()) {
        initialiseIfNeeded(o, muleContext);
      }
    }

    initialised.set(true);
  }

  @Override
  public void start() throws MuleException {
    synchronized (routes) {
      starting.set(true);
      for (Object o : getLifecycleManagedObjects()) {
        if (o instanceof Startable) {
          ((Startable) o).start();
        }
      }

      started.set(true);
      starting.set(false);
    }
  }

  @Override
  public void stop() throws MuleException {
    synchronized (routes) {
      for (Object o : getLifecycleManagedObjects()) {
        if (o instanceof Stoppable) {
          ((Stoppable) o).stop();
        }
      }

      started.set(false);
    }
  }

  @Override
  public void dispose() {
    synchronized (routes) {
      for (Object o : getLifecycleManagedObjects()) {
        if (o instanceof Disposable) {
          ((Disposable) o).dispose();
        }
      }
    }
  }

  @Override
  public void addRoute(final String expression, final Processor processor) {
    synchronized (routes) {
      ProcessorExpressionRoute addedPair = new ProcessorExpressionRoute(expression, processor);
      routes.add(transitionLifecycleManagedObjectForAddition(addedPair));
    }
  }

  @Override
  public void removeRoute(final Processor processor) {
    updateRoute(processor, index -> {
      ProcessorExpressionRoute removedPair = routes.remove(index);

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void updateRoute(final String expression, final Processor processor) {
    updateRoute(processor, index -> {
      ProcessorExpressionRoute addedPair = new ProcessorExpressionRoute(expression, processor);

      ProcessorExpressionRoute removedPair =
          routes.set(index, transitionLifecycleManagedObjectForAddition(addedPair));

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void setDefaultRoute(final Processor processor) {
    defaultProcessor = processor;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    //TODO: check with Rodro issue with merge he solved
    return Flux.merge(getPublishers(from(publisher).doOnNext(checkedConsumer(this::route))));
  }

  private List<Flux<CoreEvent>> getPublishers(Flux<CoreEvent> router) {
    List<Flux<CoreEvent>> routes = this.routes.stream()
        .map(ProcessorExpressionRoute::getPublisher)
        .collect(toCollection(ArrayList::new));
    routes.add(defaultRoute.getPublisher());
    // Phantom publisher to guarantee the router subscription occurs after all route subscriptions and with the general context
    routes.add(Flux.<CoreEvent>empty()
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> router.subscriberContext(ctx).subscribe()))));
    return routes;
  }

  protected void route(CoreEvent event) {
    if (!selectProcessor(event)) {
      defaultRoute.getSink().next(event);
    }
  }

  private Collection<?> getLifecycleManagedObjects() {
    return union(routes, singletonList(defaultRoute));
  }

  private <O> O transitionLifecycleManagedObjectForAddition(O managedObject) {
    try {
      if ((muleContext != null) && (managedObject instanceof MuleContextAware)) {
        ((MuleContextAware) managedObject).setMuleContext(muleContext);
      }

      if ((initialised.get()) && (managedObject instanceof Initialisable)) {
        ((Initialisable) managedObject).initialise();
      }

      if ((started.get()) && (managedObject instanceof Startable)) {
        ((Startable) managedObject).start();
      }
    } catch (MuleException me) {
      throw new MuleRuntimeException(me);
    }

    return managedObject;
  }

  private <O> O transitionLifecycleManagedObjectForRemoval(O managedObject) {
    try {
      if (managedObject instanceof Stoppable) {
        ((Stoppable) managedObject).stop();
      }

      if (managedObject instanceof Disposable) {
        ((Disposable) managedObject).dispose();
      }
    } catch (MuleException me) {
      throw new MuleRuntimeException(me);
    }

    return managedObject;
  }

  public void updateStatistics(Processor processor) {
    if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
      getRouterStatistics().incrementRoutedMessage(processor);
    }
  }

  private interface RoutesUpdater {

    void updateAt(int index);
  }

  private void updateRoute(Processor processor, ChoiceRouter.RoutesUpdater routesUpdater) {
    synchronized (routes) {
      for (int i = 0; i < routes.size(); i++) {
        if (routes.get(i).getMessageProcessor().equals(processor)) {
          routesUpdater.updateAt(i);
        }
      }
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

  private boolean selectProcessor(CoreEvent event) {
    try (ExpressionManagerSession emSession = expressionManager.openSession(getLocation(), event, NULL_BINDING_CONTEXT)) {
      for (ProcessorExpressionRoute cmp : routes) {
        if (emSession.evaluateBoolean(cmp.getExpression(), false, true)) {
          cmp.getSink().next(event);
          return true;
        }
      }
      return false;
    }
  }

}
