/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createNotificationExecutionTemplate;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.management.stats.RouterStatistics;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.routing.AbstractRoutingStrategy;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
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

  protected List<MessageProcessor> routes = new CopyOnWriteArrayList<>();

  protected TransactionConfig transactionConfig;

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

  private RouterStatistics routerStatistics;

  protected AtomicBoolean initialised = new AtomicBoolean(false);
  protected AtomicBoolean started = new AtomicBoolean(false);

  private MessageProcessorExecutionTemplate notificationTemplate = createNotificationExecutionTemplate();

  @Override
  public MuleEvent process(final MuleEvent event) throws MuleException {
    ExecutionTemplate<MuleEvent> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, getTransactionConfig());
    ExecutionCallback<MuleEvent> processingCallback = () -> {
      try {
        return route(event);
      } catch (RoutingException e1) {
        throw e1;
      } catch (Exception e2) {
        throw new RoutingException(event, AbstractOutboundRouter.this, e2);
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

  protected abstract MuleEvent route(MuleEvent event) throws MessagingException;

  protected final MuleEvent sendRequest(final MuleEvent originalEvent, final MuleEvent eventToRoute, final MessageProcessor route,
                                        boolean awaitResponse)
      throws MuleException {
    MuleEvent result;
    try {
      result = sendRequestEvent(originalEvent, eventToRoute, route, awaitResponse);
    } catch (MessagingException me) {
      throw me;
    } catch (Exception e) {
      throw new RoutingException(originalEvent, null, e);
    }

    if (getRouterStatistics() != null) {
      if (getRouterStatistics().isEnabled()) {
        getRouterStatistics().incrementRoutedMessage(route);
      }
    }

    if (result != null && !VoidMuleEvent.getInstance().equals(result)) {
      MuleMessage resultMessage = result.getMessage();
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
  public List<MessageProcessor> getRoutes() {
    return routes;
  }

  /*
   * For spring access
   */
  // TODO Use spring factory bean
  @Deprecated
  public void setMessageProcessors(List<MessageProcessor> routes) throws MuleException {
    setRoutes(routes);
  }

  public void setRoutes(List<MessageProcessor> routes) throws MuleException {
    this.routes.clear();
    for (MessageProcessor route : routes) {
      addRoute(route);
    }
  }

  @Override
  public synchronized void addRoute(MessageProcessor route) throws MuleException {
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
  public synchronized void removeRoute(MessageProcessor route) throws MuleException {
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
  protected MuleEvent sendRequestEvent(MuleEvent originalEvent, MuleEvent eventToRoute, MessageProcessor route,
                                       boolean awaitResponse)
      throws MuleException {
    if (route == null) {
      throw new DispatchException(CoreMessages.objectIsNull("connector operation"), originalEvent, null);
    }
    return doProcessRoute(route, eventToRoute);
  }

  protected MuleEvent doProcessRoute(MessageProcessor route, MuleEvent event) throws MuleException, MessagingException {
    if (route instanceof MessageProcessorChain) {
      return route.process(event);
    } else {
      return notificationTemplate.execute(route, event);
    }
  }

  /**
   * Create a new event to be routed to the target MP
   */
  protected MuleEvent createEventToRoute(MuleEvent routedEvent, MuleMessage message) {
    return MuleEvent.builder(routedEvent).message(message).synchronous(true).build();
  }

  /**
   * Creates a fresh copy of a {@link MuleMessage} ensuring that the payload can be cloned (i.e. is not consumable).
   *
   * @param event The {@link MuleEvent} to clone the message from.
   * @return The fresh copy of the {@link MuleMessage}.
   * @throws MessagingException If the message can't be cloned because it carries a consumable payload.
   */
  protected MuleMessage cloneMessage(MuleEvent event, MuleMessage message) throws MessagingException {
    return AbstractRoutingStrategy.cloneMessage(event, message);
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
      routes = Collections.<MessageProcessor>emptyList();
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
  protected List<MessageProcessor> getOwnedMessageProcessors() {
    return routes;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    notificationTemplate.setMuleContext(context);
  }
}
