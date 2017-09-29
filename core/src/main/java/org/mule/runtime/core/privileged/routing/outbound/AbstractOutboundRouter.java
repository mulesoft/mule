/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing.outbound;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.emptyList;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.StringMessageUtils.truncate;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.core.privileged.connector.DispatchException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.privileged.routing.OutboundRouter;
import org.mule.runtime.core.privileged.routing.RouterResultsHandler;
import org.mule.runtime.core.privileged.routing.RoutingException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statistics about message processing through the router.
 */
public abstract class AbstractOutboundRouter extends AbstractMessageProcessorOwner implements OutboundRouter {

  public static final String DEFAULT_FAILURE_EXPRESSION = "error != null";

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

  private Cache<Processor, MessageProcessorChain> processorChainCache = newBuilder().build();

  @Override
  public CoreEvent process(final CoreEvent event) throws MuleException {
    ExecutionTemplate<CoreEvent> executionTemplate =
        createTransactionalExecutionTemplate(muleContext, getTransactionConfig());
    ExecutionCallback<CoreEvent> processingCallback = () -> {
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

  protected abstract CoreEvent route(CoreEvent event) throws MuleException;

  protected final CoreEvent sendRequest(final CoreEvent event, final Processor route, boolean awaitResponse)
      throws MuleException {
    CoreEvent result;
    try {
      result = sendRequestEvent(event, route, awaitResponse);
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
      Message resultMessage = result.getMessage();
      if (logger.isTraceEnabled()) {
        if (resultMessage != null) {
          try {
            logger.trace("Response payload: \n"
                + truncate(((ExtendedTransformationService) muleContext.getTransformationService())
                    .getPayloadForLogging(resultMessage), 100, false));
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
      initialiseObject(route);
    }
    if (started.get()) {
      if (route instanceof Startable) {
        ((Startable) route).start();
      }
    }
    routes.add(route);
  }

  private void initialiseObject(Processor route) throws InitialisationException {
    initialiseIfNeeded(route, muleContext);
  }

  public TransactionConfig getTransactionConfig() {
    return transactionConfig;
  }

  @Override
  public void setTransactionConfig(TransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
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
  protected CoreEvent sendRequestEvent(CoreEvent event, Processor route, boolean awaitResponse) throws MuleException {
    if (route == null) {
      throw new DispatchException(objectIsNull("connector operation"), null);
    }
    return doProcessRoute(route, event);
  }

  protected CoreEvent doProcessRoute(Processor route, CoreEvent event) throws MuleException {
    if (route instanceof MessageProcessorChain) {
      return route.process(event);
    } else {
      // MULE-13028 All routers should use processor chains rather than processors as their routes
      MessageProcessorChain chain = processorChainCache.getIfPresent(route);
      if (chain == null) {
        chain = newChain(getProcessingStrategy(muleContext, getRootContainerName()), route);
        initialiseObject(chain);
        processorChainCache.put(route, chain);
      }
      return chain.process(event);
    }
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
      routes = emptyList();
      initialised.set(false);
      processorChainCache.invalidateAll();;
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
  }

}
