/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.routing;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.processor.MessageProcessors.newExplicitChain;
import static org.mule.runtime.core.config.i18n.CoreMessages.noEndpointsForRouter;
import static org.mule.runtime.core.routing.AbstractRoutingStrategy.validateMessageIsNotConsumable;
import static org.mule.runtime.core.util.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.util.rx.Exceptions.checkedFunction;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.AggregationContext;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.processor.chain.ExplicitMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.core.work.ProcessingMuleEventWork;
import org.mule.runtime.core.work.SerialWorkManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.WorkException;

import org.apache.commons.collections.CollectionUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The <code>Scatter-Gather</code> router will broadcast copies of the current message to every endpoint registered with the
 * router in parallel.
 * </p>
 * It is very similar to the <code>&lt;all&gt;</code> implemented in the {@link MulticastingRouter} class, except that this router
 * processes in parallel instead of sequentially.
 * <p>
 * Differences with {@link MulticastingRouter} router:
 * </p>
 * <ul>
 * <li>When using {@link MulticastingRouter} changes to the payload performed in route n are visible in route (n+1). When using
 * {@link ScatterGatherRouter}, each route has different shallow copies of the original event</li>
 * <li>{@link MulticastingRouter} throws {@link CouldNotRouteOutboundMessageException} upon route failure and stops processing.
 * When catching the exception, you'll have no information about the result of any prior routes. {@link ScatterGatherRouter} will
 * process all routes no matter what. It will also aggregate the results of all routes into a {@link Collection} in which each
 * entry has the {@link ExceptionPayload} set accordingly and then will throw a {@link CompositeRoutingException} which will give
 * you visibility over the output of other routes.</li>
 * </ul>
 * <p>
 * For advanced use cases, a custom {@link AggregationStrategy} can be applied to customize the logic used to aggregate the route
 * responses back into one single element or to throw exception
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/BroadcastAggregate.html"<a/>
 * </p>
 * 
 * @since 3.5.0
 */
public class ScatterGatherRouter extends AbstractMessageProcessorOwner implements MessageRouter {

  private static final Logger logger = LoggerFactory.getLogger(ScatterGatherRouter.class);

  /**
   * Timeout in milliseconds to be applied to each route. Values lower or equal to zero means no timeout
   */
  private long timeout = 0;

  /**
   * The routes that the message will be sent to
   */
  private List<Processor> routes = new ArrayList<>();

  /**
   * Whether or not {@link #initialise()} was already successfully executed
   */
  private boolean initialised = false;

  /**
   * chains built around the routes
   */
  private List<Processor> routeChains = emptyList();

  /**
   * The aggregation strategy. By default is this instance
   */
  private AggregationStrategy aggregationStrategy;

  /**
   * the {@link ThreadingProfile} used to create the {@link #workManager}
   */
  private ThreadingProfile threadingProfile;

  /**
   * {@link WorkManager} used to execute the routes in parallel
   */
  private WorkManager workManager;

  @Override
  public Event process(Event event) throws MuleException {
    assertMorethanOneRoute();

    InternalMessage message = event.getMessage();
    validateMessageIsNotConsumable(event, message);

    List<ProcessingMuleEventWork> works = executeWork(event);
    Event response = processResponses(event, works);

    // use a copy instead of a resetAccessControl
    // to assure that all property changes
    // are flushed from the worker thread to this one
    response = Event.builder(response).session(new DefaultMuleSession(response.getSession())).build();
    setCurrentEvent(response);

    return response;
  }

  private void assertMorethanOneRoute() throws RoutePathNotFoundException {
    if (CollectionUtils.isEmpty(routes)) {
      throw new RoutePathNotFoundException(noEndpointsForRouter(), null);
    }
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).doOnNext(checkedConsumer(event -> {
      assertMorethanOneRoute();
      validateMessageIsNotConsumable(event, event.getMessage());
    })).concatMap(event -> from(fromIterable(routeChains).concatMap(processor -> just(event).transform(processor))).collectList()
        .map(checkedFunction(list -> aggregationStrategy.aggregate(new AggregationContext(event, list)))));
  }

  private Event processResponses(Event event, List<ProcessingMuleEventWork> works)
      throws MuleException {
    List<Event> responses = new ArrayList<>(works.size());

    long remainingTimeout = timeout;
    for (int routeIndex = 0; routeIndex < works.size(); routeIndex++) {
      Event response = null;
      Exception exception = null;

      ProcessingMuleEventWork work = works.get(routeIndex);
      Processor route = routes.get(routeIndex);

      long startedAt = System.currentTimeMillis();
      try {
        response = work.getResult(remainingTimeout, TimeUnit.MILLISECONDS);
      } catch (ResponseTimeoutException e) {
        exception = e;
      } catch (InterruptedException e) {
        throw new DefaultMuleException(I18nMessageFactory.createStaticMessage(format("Was interrupted while waiting for route %d",
                                                                                     routeIndex)),
                                       e);
      } catch (MessagingException e) {
        exception = wrapInDispatchException(routeIndex, route, e);
      } catch (Exception e) {
        exception = wrapInDispatchException(routeIndex, route, e);
      }

      remainingTimeout -= System.currentTimeMillis() - startedAt;

      if (exception != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(
                       format("route %d generated exception for MuleEvent %s", routeIndex, event), exception);
        }

        if (exception instanceof MessagingException) {
          Event event1 = ((MessagingException) exception).getEvent();
          response = Event.builder(event1).session(new DefaultMuleSession(event1.getSession())).build();
        } else {
          response = Event.builder(event).session(new DefaultMuleSession(event.getSession())).build();
        }

        if (!response.getError().isPresent()) {
          event = Event.builder(event).error(response.getError().orElse(null)).build();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(format("route %d executed successfully for event %s", routeIndex, event));
        }
      }

      responses.add(response);
    }

    return aggregationStrategy.aggregate(new AggregationContext(event, responses));
  }

  private Exception wrapInDispatchException(int routeIndex, Processor route, Exception e) {
    return new DispatchException(I18nMessageFactory
        .createStaticMessage(format("route number %d failed to be executed", routeIndex)),
                                 route, e);
  }

  private List<ProcessingMuleEventWork> executeWork(Event event) throws MuleException {
    List<ProcessingMuleEventWork> works = new ArrayList<>(routes.size());
    try {
      for (final Processor route : routes) {
        ProcessingMuleEventWork work = new ProcessingMuleEventWork(route, event, muleContext, flowConstruct);
        workManager.scheduleWork(work);
        works.add(work);
      }
    } catch (WorkException e) {
      throw new DefaultMuleException(I18nMessageFactory.createStaticMessage("Could not schedule work for route"), e);
    }

    return works;
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      buildRouteChains();

      if (threadingProfile == null) {
        threadingProfile = muleContext.getDefaultThreadingProfile();
      }

      if (aggregationStrategy == null) {
        aggregationStrategy = new CollectAllAggregationStrategy();
      }

      if (timeout <= 0) {
        timeout = Long.MAX_VALUE;
      }

      if (threadingProfile.isDoThreading()) {
        workManager = threadingProfile.createWorkManager(ThreadNameHelper.getPrefix(muleContext) + "ScatterGatherWorkManager",
                                                         muleContext.getConfiguration().getShutdownTimeout());
      } else {
        workManager = new SerialWorkManager();
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    super.initialise();
    initialised = true;
  }

  @Override
  public void start() throws MuleException {
    workManager.start();
    super.start();
  }

  @Override
  public void dispose() {
    try {
      workManager.dispose();
    } catch (Exception e) {
      logger.error(
                   "Exception found while tring to dispose work manager. Will continue with the disposal", e);
    } finally {
      super.dispose();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IllegalStateException if invoked after {@link #initialise()} is completed
   */
  @Override
  public void addRoute(Processor processor) throws MuleException {
    checkNotInitialised();
    routes.add(processor);
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IllegalStateException if invoked after {@link #initialise()} is completed
   */
  @Override
  public void removeRoute(Processor processor) throws MuleException {
    checkNotInitialised();
    routes.remove(processor);
  }

  private void buildRouteChains() {
    Preconditions.checkState(routes.size() > 1, "At least 2 routes are required for ScatterGather");
    routeChains = routes.stream().map(route -> route instanceof ExplicitMessageProcessorChainBuilder.ExplicitMessageProcessorChain
        ? (MessageProcessorChain) route : newExplicitChain(route)).collect(toList());
  }

  private void checkNotInitialised() {
    Preconditions.checkState(initialised == false,
                             "<scatter-gather> router is not dynamic. Cannot modify routes after initialisation");
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return routeChains;
  }

  public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
    this.aggregationStrategy = aggregationStrategy;
  }

  public void setThreadingProfile(ThreadingProfile threadingProfile) {
    this.threadingProfile = threadingProfile;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public void setRoutes(List<Processor> routes) {
    this.routes = routes;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    pathElement = pathElement.addChild(this);
    for (Processor route : routeChains) {
      NotificationUtils.addMessageProcessorPathElements(route, pathElement);
    }
  }

}
