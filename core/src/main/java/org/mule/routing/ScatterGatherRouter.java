/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageRouter;
import org.mule.api.routing.AggregationContext;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.util.Preconditions;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.work.ProcessingMuleEventWork;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.WorkException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The <code>Scatter-Gather</code> router will broadcast copies of the current
 * message to every endpoint registered with the router in parallel.
 * </p>
 * It is very similar to the <code>&lt;all&gt;</code> implemented in the
 * {@link MulticastingRouter} class, except that this router processes in parallel
 * instead of sequentially.
 * <p>
 * Differences with {@link MulticastingRouter} router:
 * </p>
 * <ul>
 * <li>When using {@link MulticastingRouter} changes to the payload performed in
 * route n are visible in route (n+1). When using {@link ScatterGatherRouter}, each
 * route has different shallow copies of the original event</li>
 * <li> {@link MulticastingRouter} throws
 * {@link CouldNotRouteOutboundMessageException} upon route failure and stops
 * processing. When catching the exception, you'll have no information about the
 * result of any prior routes. {@link ScatterGatherRouter} will process all routes no
 * matter what. It will also aggregate the results of all routes into a
 * {@link MuleMessageCollection} in which each entry has the {@link ExceptionPayload}
 * set accordingly and then will throw a {@link CompositeRoutingException} which will
 * give you visibility over the output of other routes.</li>
 * </ul>
 * <p>
 * For advanced use cases, a custom {@link AggregationStrategy} can be applied to
 * customize the logic used to aggregate the route responses back into one single
 * element or to throw exception
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/BroadcastAggregate.html"<a/>
 * </p>
 * 
 * @since 3.5.0
 */
public class ScatterGatherRouter extends AbstractMessageProcessorOwner implements MessageRouter
{

    private static final Logger logger = LoggerFactory.getLogger(ScatterGatherRouter.class);

    /**
     * Timeout in milliseconds to be applied to each route. Values lower or equal to
     * zero means no timeout
     */
    private long timeout = 0;

    /**
     * The routes that the message will be sent to
     */
    private List<MessageProcessor> routes = new ArrayList<MessageProcessor>();

    /**
     * Whether or not {@link #initialise()} was already successfully executed
     */
    private boolean initialised = false;

    /**
     * chains built around the routes
     */
    private List<MessageProcessor> routeChains;

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
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (CollectionUtils.isEmpty(routes))
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        MuleMessage message = event.getMessage();
        AbstractRoutingStrategy.validateMessageIsNotConsumable(event, message);

        List<ProcessingMuleEventWork> works = executeWork(event);
        MuleEvent response = processResponses(event, works);

        if (response instanceof DefaultMuleEvent)
        {
            // use a copy instead of a resetAccessControl
            // to assure that all property changes
            // are flushed from the worker thread to this one
            response = DefaultMuleEvent.copy(response);
        }

        return response;
    }

    private MuleEvent processResponses(MuleEvent event, List<ProcessingMuleEventWork> works)
        throws MuleException
    {
        List<MuleEvent> responses = new ArrayList<MuleEvent>(works.size());

        long remainingTimeout = timeout;
        for (int routeIndex = 0; routeIndex < works.size(); routeIndex++)
        {
            MuleEvent response = null;
            Exception exception = null;

            ProcessingMuleEventWork work = works.get(routeIndex);
            MessageProcessor route = routes.get(routeIndex);

            long startedAt = System.currentTimeMillis();
            try
            {
                response = work.getResult(remainingTimeout, TimeUnit.MILLISECONDS);
            }
            catch (ResponseTimeoutException e)
            {
                exception = e;
            }
            catch (InterruptedException e)
            {
                throw new DefaultMuleException(MessageFactory.createStaticMessage(String.format(
                    "Was interrupted while waiting for route %d", routeIndex)), e);
            }
            catch (Exception e)
            {
                exception = new DispatchException(MessageFactory.createStaticMessage(String.format(
                    "route number %d failed to be executed", routeIndex)), event, route, exception);
            }

            remainingTimeout -= System.currentTimeMillis() - startedAt;

            if (exception != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                        String.format("route %d generated exception for MuleEvent %s", routeIndex,
                            event.getId()), exception);
                }
                response = DefaultMuleEvent.copy(event);
                response.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("route %d executed successfully for event %s", routeIndex,
                        event.getId()));
                }
            }

            responses.add(response);
        }

        return aggregationStrategy.aggregate(new AggregationContext(event, responses));
    }

    private List<ProcessingMuleEventWork> executeWork(MuleEvent event) throws MuleException
    {
        List<ProcessingMuleEventWork> works = new ArrayList<ProcessingMuleEventWork>(routes.size());
        try
        {
            for (final MessageProcessor route : routes)
            {
                ProcessingMuleEventWork work = new ProcessingMuleEventWork(route, event);
                workManager.scheduleWork(work);
                works.add(work);
            }
        }
        catch (WorkException e)
        {
            throw new DefaultMuleException(
                MessageFactory.createStaticMessage("Could not schedule work for route"), e);
        }

        return works;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            buildRouteChains();

            if (threadingProfile == null)
            {
                threadingProfile = muleContext.getDefaultThreadingProfile();
            }

            if (aggregationStrategy == null)
            {
                aggregationStrategy = new CollectAllAggregationStrategy();
            }

            if (timeout <= 0)
            {
                timeout = Long.MAX_VALUE;
            }

            workManager = threadingProfile.createWorkManager(
                ThreadNameHelper.getPrefix(muleContext) + "ScatterGatherWorkManager",
                muleContext.getConfiguration().getShutdownTimeout());
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

        super.initialise();
        initialised = true;
    }

    @Override
    public void start() throws MuleException
    {
        workManager.start();
        super.start();
    }

    @Override
    public void dispose()
    {
        try
        {
            workManager.dispose();
        }
        catch (Exception e)
        {
            logger.error(
                "Exception found while tring to dispose work manager. Will continue with the disposal", e);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException if invoked after {@link #initialise()} is
     *             completed
     */
    @Override
    public void addRoute(MessageProcessor processor) throws MuleException
    {
        checkNotInitialised();
        routes.add(processor);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException if invoked after {@link #initialise()} is
     *             completed
     */
    @Override
    public void removeRoute(MessageProcessor processor) throws MuleException
    {
        checkNotInitialised();
        routes.remove(processor);
    }

    private void buildRouteChains() throws MuleException
    {
        Preconditions.checkState(routes.size() > 1, "At least 2 routes are required for ScatterGather");
        routeChains = new ArrayList<MessageProcessor>(routes.size());
        for (MessageProcessor route : routes)
        {
            routeChains.add(new DefaultMessageProcessorChainBuilder().chain(route).build());
        }
    }

    private void checkNotInitialised()
    {
        Preconditions.checkState(initialised == false,
            "<scatter-gather> router is not dynamic. Cannot modify routes after initialisation");
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return routes;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy)
    {
        this.aggregationStrategy = aggregationStrategy;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public void setRoutes(List<MessageProcessor> routes)
    {
        this.routes = routes;
    }
}
