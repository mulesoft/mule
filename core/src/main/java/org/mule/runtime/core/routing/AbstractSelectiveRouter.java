/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections.ListUtils.union;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setFlowConstructIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.api.routing.RouterStatisticsRecorder;
import org.mule.runtime.core.api.routing.SelectiveRouter;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.management.stats.RouterStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

public abstract class AbstractSelectiveRouter extends AbstractAnnotatedObject implements SelectiveRouter,
    RouterStatisticsRecorder, Lifecycle, FlowConstructAware, MuleContextAware {

  private final List<MessageProcessorFilterPair> conditionalMessageProcessors = new ArrayList<>();
  private Processor defaultProcessor;
  private final RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  private RouterStatistics routerStatistics;

  final AtomicBoolean initialised = new AtomicBoolean(false);
  final AtomicBoolean starting = new AtomicBoolean(false);
  final AtomicBoolean started = new AtomicBoolean(false);
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;

  public AbstractSelectiveRouter() {
    routerStatistics = new RouterStatistics(TYPE_OUTBOUND);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
    conditionalMessageProcessors.forEach(pair -> pair.setFlowConstruct(flowConstruct));
    conditionalMessageProcessors.forEach(pair -> pair.setMuleContext(muleContext));
    setMuleContextIfNeeded(defaultProcessor, muleContext);
    setFlowConstructIfNeeded(defaultProcessor, flowConstruct);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    synchronized (conditionalMessageProcessors) {
      for (Object o : getLifecycleManagedObjects()) {
        if (o instanceof FlowConstructAware) {
          ((FlowConstructAware) o).setFlowConstruct(flowConstruct);
        }
        if (o instanceof MuleContextAware) {
          ((MuleContextAware) o).setMuleContext(muleContext);
        }
        if (o instanceof Initialisable) {
          ((Initialisable) o).initialise();
        }
      }
    }
    initialised.set(true);
  }

  @Override
  public void start() throws MuleException {
    synchronized (conditionalMessageProcessors) {
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
    synchronized (conditionalMessageProcessors) {
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
    synchronized (conditionalMessageProcessors) {
      for (Object o : getLifecycleManagedObjects()) {
        if (o instanceof Disposable) {
          ((Disposable) o).dispose();
        }
      }
    }
  }

  @Override
  public void addRoute(Processor processor, Filter filter) {
    synchronized (conditionalMessageProcessors) {
      MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);
      conditionalMessageProcessors.add(transitionLifecycleManagedObjectForAddition(addedPair));
    }
  }

  @Override
  public void removeRoute(Processor processor) {
    updateRoute(processor, (RoutesUpdater) index -> {
      MessageProcessorFilterPair removedPair = conditionalMessageProcessors.remove(index);

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void updateRoute(final Processor processor, final Filter filter) {
    updateRoute(processor, (RoutesUpdater) index -> {
      MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);

      MessageProcessorFilterPair removedPair =
          conditionalMessageProcessors.set(index, transitionLifecycleManagedObjectForAddition(addedPair));

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void setDefaultRoute(Processor processor) {
    defaultProcessor = processor;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return routeWithProcessors(getProcessorsToRoute(event), event);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).concatMap(event -> {
      try {
        return fromIterable(getProcessorsToRoute(event))
            .concatMap(mp -> just(event).transform(mp))
            .collectList()
            .handle((list, sink) -> {
              Event aggregateEvent = resultsHandler.aggregateResults(list, event);
              // Routes will also return null when an exception is thrown, so we need to take into account this case and not
              // continue processing if the response has already been completed.
              if (aggregateEvent != null && !Mono.from(event.getContext().getResponsePublisher()).toFuture().isDone()) {
                sink.next(aggregateEvent);
              }
            });
      } catch (RoutePathNotFoundException e) {
        return error(new MessagingException(event, e, this));
      }
    });
  }

  protected Collection<Processor> getProcessorsToRoute(Event event) throws RoutePathNotFoundException {
    Collection<Processor> selectedProcessors = selectProcessors(event, Event.builder(event));
    if (!selectedProcessors.isEmpty()) {
      return selectedProcessors;
    } else if (defaultProcessor != null) {
      return singleton(defaultProcessor);
    } else {
      if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
        getRouterStatistics().incrementNoRoutedMessage();
      }

      throw new RoutePathNotFoundException(createStaticMessage("Can't process message because no route has been found matching any filter and no default route is defined"),
                                           this);
    }
  }

  /**
   * @return the processors selected according to the specific router strategy or an empty collection (not null).
   */
  protected abstract Collection<Processor> selectProcessors(Event event, Event.Builder builder);

  private Collection<?> getLifecycleManagedObjects() {
    if (defaultProcessor == null) {
      return conditionalMessageProcessors;
    }

    return union(conditionalMessageProcessors, singletonList(defaultProcessor));
  }

  private <O> O transitionLifecycleManagedObjectForAddition(O managedObject) {
    try {
      if ((flowConstruct != null) && (managedObject instanceof FlowConstructAware)) {
        ((FlowConstructAware) managedObject).setFlowConstruct(flowConstruct);
      }

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

  private Event routeWithProcessors(Collection<Processor> processors, Event event) throws MuleException {
    List<Event> results = new ArrayList<>();

    for (Processor processor : processors) {
      processEventWithProcessor(event, processor, results);
    }

    return resultsHandler.aggregateResults(results, event);
  }

  private void processEventWithProcessor(Event event, Processor processor, List<Event> results)
      throws MuleException {
    results.add(processor.process(event));

    if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
      getRouterStatistics().incrementRoutedMessage(processor);
    }
  }

  public List<MessageProcessorFilterPair> getConditionalMessageProcessors() {
    return unmodifiableList(conditionalMessageProcessors);
  }

  private interface RoutesUpdater {

    void updateAt(int index);
  }

  private void updateRoute(Processor processor, RoutesUpdater routesUpdater) {
    synchronized (conditionalMessageProcessors) {
      for (int i = 0; i < conditionalMessageProcessors.size(); i++) {
        if (conditionalMessageProcessors.get(i).getMessageProcessor().equals(processor)) {
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
    return String.format("%s [flow-construct=%s, started=%s]", getClass().getSimpleName(),
                         flowConstruct != null ? flowConstruct.getName() : null, started);
  }
}
