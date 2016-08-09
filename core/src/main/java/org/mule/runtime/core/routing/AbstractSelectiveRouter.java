/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.api.routing.RouterStatisticsRecorder;
import org.mule.runtime.core.api.routing.SelectiveRouter;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.management.stats.RouterStatistics;
import org.mule.runtime.core.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.ListUtils;

public abstract class AbstractSelectiveRouter extends AbstractAnnotatedObject implements SelectiveRouter,
    RouterStatisticsRecorder, Lifecycle, FlowConstructAware, MuleContextAware, MessageProcessorContainer {

  private final List<MessageProcessorFilterPair> conditionalMessageProcessors = new ArrayList<>();
  private MessageProcessor defaultProcessor;
  private final RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  private RouterStatistics routerStatistics;

  final AtomicBoolean initialised = new AtomicBoolean(false);
  final AtomicBoolean starting = new AtomicBoolean(false);
  final AtomicBoolean started = new AtomicBoolean(false);
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;

  public AbstractSelectiveRouter() {
    routerStatistics = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
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
  public void addRoute(MessageProcessor processor, Filter filter) {
    synchronized (conditionalMessageProcessors) {
      MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);
      conditionalMessageProcessors.add(transitionLifecycleManagedObjectForAddition(addedPair));
    }
  }

  @Override
  public void removeRoute(MessageProcessor processor) {
    updateRoute(processor, (RoutesUpdater) index -> {
      MessageProcessorFilterPair removedPair = conditionalMessageProcessors.remove(index);

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void updateRoute(final MessageProcessor processor, final Filter filter) {
    updateRoute(processor, (RoutesUpdater) index -> {
      MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);

      MessageProcessorFilterPair removedPair =
          conditionalMessageProcessors.set(index, transitionLifecycleManagedObjectForAddition(addedPair));

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void setDefaultRoute(MessageProcessor processor) {
    defaultProcessor = processor;
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    Collection<MessageProcessor> selectedProcessors = selectProcessors(event);

    if (!selectedProcessors.isEmpty()) {
      return routeWithProcessors(selectedProcessors, event);
    }

    if (defaultProcessor != null) {
      return routeWithProcessor(defaultProcessor, event);
    }

    if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
      getRouterStatistics().incrementNoRoutedMessage();
    }

    throw new RoutePathNotFoundException(MessageFactory
        .createStaticMessage("Can't process message because no route has been found matching any filter and no default route is defined"),
                                         event, this);
  }

  /**
   * @return the processors selected according to the specific router strategy or an empty collection (not null).
   */
  protected abstract Collection<MessageProcessor> selectProcessors(MuleEvent event);

  private Collection<?> getLifecycleManagedObjects() {
    if (defaultProcessor == null) {
      return conditionalMessageProcessors;
    }

    return ListUtils.union(conditionalMessageProcessors, Collections.singletonList(defaultProcessor));
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

  private MuleEvent routeWithProcessor(MessageProcessor processor, MuleEvent event) throws MuleException {
    return routeWithProcessors(Collections.singleton(processor), event);
  }

  private MuleEvent routeWithProcessors(Collection<MessageProcessor> processors, MuleEvent event) throws MuleException {
    List<MuleEvent> results = new ArrayList<>();

    for (MessageProcessor processor : processors) {
      processEventWithProcessor(event, processor, results);
    }

    return resultsHandler.aggregateResults(results, event);
  }

  private void processEventWithProcessor(MuleEvent event, MessageProcessor processor, List<MuleEvent> results)
      throws MuleException {
    results.add(processor.process(event));

    if (getRouterStatistics() != null && getRouterStatistics().isEnabled()) {
      getRouterStatistics().incrementRoutedMessage(processor);
    }
  }

  public List<MessageProcessorFilterPair> getConditionalMessageProcessors() {
    return Collections.unmodifiableList(conditionalMessageProcessors);
  }

  private interface RoutesUpdater {

    void updateAt(int index);
  }

  private void updateRoute(MessageProcessor processor, RoutesUpdater routesUpdater) {
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
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    List<MessageProcessor> messageProcessors = new ArrayList<>();
    for (MessageProcessorFilterPair cmp : conditionalMessageProcessors) {
      messageProcessors.add(cmp.getMessageProcessor());
    }
    messageProcessors.add(defaultProcessor);
    NotificationUtils.addMessageProcessorPathElements(messageProcessors, pathElement);
  }

  @Override
  public String toString() {
    return String.format("%s [flow-construct=%s, started=%s]", getClass().getSimpleName(),
                         flowConstruct != null ? flowConstruct.getName() : null, started);
  }
}
