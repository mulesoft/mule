/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.routing;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.newExplicitChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.config.i18n.CoreMessages.noEndpointsForRouter;
import static org.mule.runtime.core.internal.util.ProcessingStrategyUtils.isSynchronousProcessing;
import static org.mule.runtime.core.routing.AbstractRoutingStrategy.validateMessageIsNotConsumable;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.routing.AggregationContext;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

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

  @Inject
  private SchedulerService schedulerService;

  /**
   * Whether the configured routes will run in parallel (default is true).
   */
  private boolean parallel = true;

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

  private Scheduler scheduler;
  private reactor.core.scheduler.Scheduler reactorScheduler;

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
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
    })).concatMap(event -> from(fromIterable(routeChains).concatMap(processor -> just(event).transform(scheduleRoute(processor))))
        .collectList()
        .map(checkedFunction(list -> aggregationStrategy.aggregate(new AggregationContext(event, list)))));
  }

  private ReactiveProcessor scheduleRoute(Processor route) {
    if (!isSynchronousProcessing(flowConstruct) && flowConstruct instanceof Pipeline) {
      // If an async processing strategy is in use then use it to schedule scatter-gather route
      return publisher -> from(publisher).transform(((Pipeline) flowConstruct).getProcessingStrategy().onPipeline(route));
    } else if (reactorScheduler != null) {
      // Otherwise schedule async processing on an IO thread.
      return publisher -> from(publisher).transform(route).subscribeOn(reactorScheduler);
    } else {
      return publisher -> from(publisher).transform(route);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      buildRouteChains();

      if (aggregationStrategy == null) {
        aggregationStrategy = new CollectAllAggregationStrategy();
      }

      if (timeout <= 0) {
        timeout = Long.MAX_VALUE;
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    super.initialise();
    initialised = true;
  }

  @Override
  public void start() throws MuleException {
    scheduler = schedulerService.ioScheduler(getLocation() != null
        ? muleContext.getSchedulerBaseConfig().withName(getLocation().getLocation()) : muleContext.getSchedulerBaseConfig());
    reactorScheduler = fromExecutorService(scheduler);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    super.stop();
    scheduler.stop();
    reactorScheduler.dispose();
    scheduler = null;
    reactorScheduler = null;
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
    // Wrap in explicit chain
    routeChains = routes.stream().map(route -> newChain(newExplicitChain(route))).collect(toList());
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

  public void setParallel(boolean parallel) {
    this.parallel = parallel;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public void setRoutes(List<Processor> routes) {
    this.routes = routes;
  }

}
