/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageRouter;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.routing.outbound.MulticastingRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.collections.CollectionUtils;

/**
 * <p>
 * The <code>Scatter-Gather</code> router will broadcast copies current message to
 * every endpoint registered with the router in parallel.
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
 * For advanced use cases, a custom {@link EventMergeStrategy} can be applied to
 * customize the logic used to merge the response events
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/BroadcastAggregate.html"<a/>
 * </p>
 * 
 * @since 3.5.0
 */
public class ScatterGatherRouter extends AbstractMessageProcessorOwner implements MessageRouter
{

    private static final EventMergeStrategy DEFAULT_MERGE_STRATEGY = new ScatterGatherEventMergeStrategy();

    /**
     * Timeout in milliseconds to be applied to each route
     */
    private long timeout = 0;
    private List<MessageProcessor> routes = new CopyOnWriteArrayList<MessageProcessor>();
    private EventMergeStrategy eventMergeStrategy;
    private ThreadingProfile threadingProfile;
    private ExecutorService executorService;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (CollectionUtils.isEmpty(this.routes))
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        MuleMessage message = event.getMessage();
        AbstractRoutingStrategy.validateMessageIsNotConsumable(event, message);

        List<Future<MuleEvent>> futures = this.executeRoutes(event);
        List<MuleEvent> responses = this.getResponses(event, futures);

        return this.eventMergeStrategy.merge(event, responses);
    }

    @SuppressWarnings("unchecked")
    private List<MuleEvent> getResponses(MuleEvent event, List<Future<MuleEvent>> futures)
        throws ResponseTimeoutException, MessagingException
    {
        List<MuleEvent> responses = new ArrayList<MuleEvent>(futures.size());
        int routeIndex = 0;

        for (Future<MuleEvent> future : futures)
        {
            MuleEvent response = null;
            Exception exception = null;
            MessageProcessor route = this.routes.get(routeIndex++);

            try
            {
                response = future.get(this.timeout, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                Message message = MessageFactory.createStaticMessage(String.format(
                    "route number %d timed out", routeIndex));

                exception = new ResponseTimeoutException(message, event, route);
            }
            catch (CancellationException e)
            {
                exception = new DispatchException(MessageFactory.createStaticMessage(String.format(
                    "route number %d was cancelled", routeIndex)), event, route, exception);
            }
            catch (ExecutionException e)
            {
                exception = new DispatchException(MessageFactory.createStaticMessage(String.format(
                    "route number %d failed to be executed", routeIndex)), event, route, exception);
            }
            catch (InterruptedException e)
            {
                exception = e;
            }

            if (exception != null)
            {
                response = DefaultMuleEvent.copy(event);
                ExceptionPayload ep = new DefaultExceptionPayload(exception);

                ep.getInfo().put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, routeIndex);
                response.getMessage().setExceptionPayload(ep);
            }

            responses.add(response);
        }

        return responses;
    }

    private List<Future<MuleEvent>> executeRoutes(final MuleEvent event) throws MessagingException
    {
        List<Callable<MuleEvent>> executors = new ArrayList<Callable<MuleEvent>>(this.routes.size());
        for (final MessageProcessor route : this.routes)
        {
            executors.add(new Callable<MuleEvent>()
            {
                @Override
                public MuleEvent call() throws Exception
                {
                    return route.process(DefaultMuleEvent.copy(event));
                }
            });
        }

        List<Future<MuleEvent>> futures = new ArrayList<Future<MuleEvent>>(executors.size());

        for (Callable<MuleEvent> executor : executors)
        {
            futures.add(this.executorService.submit(executor));
        }

        return futures;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (this.threadingProfile == null)
        {
            this.threadingProfile = this.muleContext.getDefaultThreadingProfile();
        }

        if (this.eventMergeStrategy == null)
        {
            this.eventMergeStrategy = DEFAULT_MERGE_STRATEGY;
        }

        if (this.timeout <= 0)
        {
            this.timeout = Long.MAX_VALUE;
        }

        this.executorService = this.threadingProfile.createPool();
        if (this.executorService instanceof Initialisable)
        {
            ((Initialisable) this.executorService).initialise();
        }

        super.initialise();
    }

    @Override
    public void dispose()
    {
        try
        {
            this.executorService.shutdown();
        }
        finally
        {
            super.dispose();
        }
    }

    @Override
    public void addRoute(MessageProcessor processor) throws MuleException
    {
        this.routes.add(processor);
    }

    @Override
    public void removeRoute(MessageProcessor processor) throws MuleException
    {
        this.routes.remove(processor);
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return this.routes;
    }

    public void setEventMergeStrategy(EventMergeStrategy mergeStrategy)
    {
        this.eventMergeStrategy = mergeStrategy;
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
