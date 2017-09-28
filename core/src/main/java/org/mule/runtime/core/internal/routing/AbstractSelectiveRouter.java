/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.ListUtils.union;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

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
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;
import org.mule.runtime.core.privileged.routing.RouterStatisticsRecorder;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractSelectiveRouter extends AbstractComponent implements SelectiveRouter,
    RouterStatisticsRecorder, Lifecycle, MuleContextAware {

  private final List<MessageProcessorExpressionPair> conditionalMessageProcessors = new ArrayList<>();
  private Optional<Processor> defaultProcessor = empty();
  private RouterStatistics routerStatistics;

  final AtomicBoolean initialised = new AtomicBoolean(false);
  final AtomicBoolean starting = new AtomicBoolean(false);
  final AtomicBoolean started = new AtomicBoolean(false);
  private MuleContext muleContext;

  public AbstractSelectiveRouter() {
    routerStatistics = new RouterStatistics(TYPE_OUTBOUND);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    synchronized (conditionalMessageProcessors) {
      for (Object o : getLifecycleManagedObjects()) {
        initialiseIfNeeded(o, muleContext);
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
  public void addRoute(final String expression, final Processor processor) {
    synchronized (conditionalMessageProcessors) {
      MessageProcessorExpressionPair addedPair = new MessageProcessorExpressionPair(expression, processor);
      conditionalMessageProcessors.add(transitionLifecycleManagedObjectForAddition(addedPair));
    }
  }

  @Override
  public void removeRoute(final Processor processor) {
    updateRoute(processor, index -> {
      MessageProcessorExpressionPair removedPair = conditionalMessageProcessors.remove(index);

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void updateRoute(final String expression, final Processor processor) {
    updateRoute(processor, index -> {
      MessageProcessorExpressionPair addedPair = new MessageProcessorExpressionPair(expression, processor);

      MessageProcessorExpressionPair removedPair =
          conditionalMessageProcessors.set(index, transitionLifecycleManagedObjectForAddition(addedPair));

      transitionLifecycleManagedObjectForRemoval(removedPair);
    });
  }

  @Override
  public void setDefaultRoute(final Processor processor) {
    defaultProcessor = ofNullable(processor);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher).flatMap(checkedFunction(event -> {
      Processor processor = getProcessorToRoute(event);
      return just(event).transform(processor).doOnComplete(() -> updateStatistics(processor));
    }));
  }

  protected Processor getProcessorToRoute(CoreEvent event) throws RoutePathNotFoundException {
    Optional<Processor> selectedProcessor = selectProcessor(event);
    return (selectedProcessor.isPresent() ? selectedProcessor : defaultProcessor)
        .orElseThrow(() -> new RoutePathNotFoundException(createStaticMessage("Can't process message because no route has been found matching any filter and no default route is defined"),
                                                          this));
  }

  /**
   * @return the processor selected according to the specific router strategy.
   */
  protected abstract Optional<Processor> selectProcessor(CoreEvent event);

  private Collection<?> getLifecycleManagedObjects() {
    if (!defaultProcessor.isPresent()) {
      return conditionalMessageProcessors;
    }

    return union(conditionalMessageProcessors, singletonList(defaultProcessor.get()));
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

  public List<MessageProcessorExpressionPair> getConditionalMessageProcessors() {
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
    return format("%s [flow=%s, started=%s]", getClass().getSimpleName(), getLocation().getRootContainerName(), started);
  }

}
