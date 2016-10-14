/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createNotificationExecutionTemplate;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.management.stats.RouterStatistics;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.routing.AbstractRoutingStrategy;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statistics about message processing through the router.
 */
public abstract class AbstractOutboundRouter extends AbstractMessageProcessorOwner implements OutboundRouter {

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected List<Processor> routes = new CopyOnWriteArrayList<>();

  protected TransactionConfig transactionConfig;

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

  private RouterStatistics routerStatistics;

  protected AtomicBoolean initialised = new AtomicBoolean(false);
  protected AtomicBoolean started = new AtomicBoolean(false);

  private MessageProcessorExecutionTemplate notificationTemplate = createNotificationExecutionTemplate();

  @Override
  public Event process(final Event event) throws MuleException {
    ExecutionTemplate<Event> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, getTransactionConfig());
    ExecutionCallback<Event> processingCallback = () -> {
      try {
        return route(event);
      } catch (RoutingException e1) {
        throw e1;
      } catch (Exception e2) {
        throw new RoutingException(AbstractOutboundRouter.this, e2);
      }
    };
    try {
      return executionTemplate.execute(processingCallback);
    } catch (MuleException e) {
      throw e;
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  protected abstract Event route(Event event) throws MuleException;

  protected final Event sendRequest(final Event originalEvent, final Event eventToRoute, final Processor route,
                                    boolean awaitResponse)
      throws MuleException {
    Event result;
    try {
      result = sendRequestEvent(originalEvent, eventToRoute, route, awaitResponse);
    } catch (MessagingException me) {
      throw me;
    } catch (Exception e) {
      throw new RoutingException(null, e);
    }

    if (getRouterStatistics() != null) {
      if (getRouterStatistics().isEnabled()) {
        getRouterStatistics().incrementRoutedMessage(route);
      }
    }

    if (result != null) {
      InternalMessage resultMessage = result.getMessage();
      if (logger.isTraceEnabled()) {
        if (resultMessage != null) {
          try {
            logger.trace("Response payload: \n" + StringMessageUtils
                .truncate(muleContext.getTransformationService().getPayloadForLogging(resultMessage), 100, false));
          } catch (Exception e) {
            logger.trace("Response payload: \n(unable to retrieve payload: " + e.getMessage());
          }
        }
      }
    }

    return result;
  }

  @Override
  public List<Processor> getRoutes() {
    return routes;
  }

  /*
   * For spring access
   */
  // TODO Use spring factory bean
  @Deprecated
  public void setMessageProcessors(List<Processor> routes) throws MuleException {
    setRoutes(routes);
  }

  public void setRoutes(List<Processor> routes) throws MuleException {
    this.routes.clear();
    for (Processor route : routes) {
      addRoute(route);
    }
  }

  @Override
  public synchronized void addRoute(Processor route) throws MuleException {
    if (initialised.get()) {
      if (route instanceof MuleContextAware) {
        ((MuleContextAware) route).setMuleContext(muleContext);
      }
      if (route instanceof FlowConstructAware) {
        ((FlowConstructAware) route).setFlowConstruct(flowConstruct);
      }
      if (route instanceof Initialisable) {
        ((Initialisable) route).initialise();
      }
    }
    if (started.get()) {
      if (route instanceof Startable) {
        ((Startable) route).start();
      }
    }
    routes.add(route);
  }

  @Override
  public synchronized void removeRoute(Processor route) throws MuleException {
    if (started.get()) {
      if (route instanceof Stoppable) {
        ((Stoppable) route).stop();
      }
    }
    if (initialised.get()) {
      if (route instanceof Disposable) {
        ((Disposable) route).dispose();
      }
    }
    routes.remove(route);
  }

  public TransactionConfig getTransactionConfig() {
    return transactionConfig;
  }

  @Override
  public void setTransactionConfig(TransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
  }

  @Override
  public boolean isDynamicRoutes() {
    return false;
  }

  public RouterResultsHandler getResultsHandler() {
    return resultsHandler;
  }

  public void setResultsHandler(RouterResultsHandler resultsHandler) {
    this.resultsHandler = resultsHandler;
  }

  /**
   * Send message event to destination.
   */
  protected Event sendRequestEvent(Event originalEvent, Event eventToRoute, Processor route,
                                   boolean awaitResponse)
      throws MuleException {
    if (route == null) {
      throw new DispatchException(CoreMessages.objectIsNull("connector operation"), null);
    }
    return doProcessRoute(route, eventToRoute);
  }

  protected Event doProcessRoute(Processor route, Event event) throws MuleException, MessagingException {
    if (route instanceof MessageProcessorChain) {
      return route.process(event);
    } else {
      return notificationTemplate.execute(route, event);
    }
  }

  /**
   * Create a new event to be routed to the target MP
   */
  protected Event createEventToRoute(Event routedEvent, InternalMessage message) {
    return Event.builder(routedEvent).message(message).synchronous(true).build();
  }

  /**
   * Creates a fresh copy of a {@link Message} ensuring that the payload can be cloned (i.e. is not consumable).
   *
   * @param event The {@link Event} to clone the message from.
   * @return The fresh copy of the {@link Message}.
   * @throws MessagingException If the message can't be cloned because it carries a consumable payload.
   */
  protected InternalMessage cloneMessage(Event event, InternalMessage message) throws MuleException {
    return AbstractRoutingStrategy.cloneMessage(message);
  }

  @Override
  public void initialise() throws InitialisationException {
    synchronized (routes) {
      super.initialise();
      initialised.set(true);
    }
  }

  @Override
  public void dispose() {
    synchronized (routes) {
      super.dispose();
      routes = Collections.<Processor>emptyList();
      initialised.set(false);
    }
  }

  @Override
  public void start() throws MuleException {
    synchronized (routes) {
      super.start();
      started.set(true);
    }
  }

  @Override
  public void stop() throws MuleException {
    synchronized (routes) {
      super.stop();
      started.set(false);
    }
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public void setRouterStatistics(RouterStatistics stats) {
    this.routerStatistics = stats;
  }

  public RouterStatistics getRouterStatistics() {
    return routerStatistics;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return routes;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    notificationTemplate.setMuleContext(context);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    super.setFlowConstruct(flowConstruct);
    notificationTemplate.setFlowConstruct(flowConstruct);
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(getOwnedMessageProcessors(), pathElement.addChild(this));
  }

}
