/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
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
import org.mule.runtime.core.routing.CorrelationMode;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.SystemUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statistics about message processing
 * through the router.
 */
public abstract class AbstractOutboundRouter extends AbstractMessageProcessorOwner implements OutboundRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List<MessageProcessor> routes = new CopyOnWriteArrayList<MessageProcessor>();

    protected String replyTo = null;

    /**
     * Determines if Mule stamps outgoing message with a correlation ID or not.
     */
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;

    protected TransactionConfig transactionConfig;

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

    private RouterStatistics routerStatistics;

    protected AtomicBoolean initialised = new AtomicBoolean(false);
    protected AtomicBoolean started = new AtomicBoolean(false);

    private MessageProcessorExecutionTemplate notificationTemplate = MessageProcessorExecutionTemplate.createNotificationExecutionTemplate();

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        ExecutionTemplate<MuleEvent> executionTemplate = TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, getTransactionConfig());
        ExecutionCallback<MuleEvent> processingCallback = new ExecutionCallback<MuleEvent>()
        {
            @Override
            public MuleEvent process() throws Exception
            {
                try
                {
                    return route(event);
                }
                catch (RoutingException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new RoutingException(event, AbstractOutboundRouter.this, e);
                }
            }
        };
        try
        {
            return executionTemplate.execute(processingCallback);
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    protected abstract MuleEvent route(MuleEvent event) throws MessagingException;

    protected final MuleEvent sendRequest(final MuleEvent originalEvent,
                                          final MuleEvent eventToRoute,
                                          final MessageProcessor route,
                                          boolean awaitResponse) throws MuleException
    {
        if (awaitResponse && replyTo != null)
        {
            logger.debug("event was dispatched synchronously, but there is a ReplyTo route set, so using asynchronous dispatch");
            awaitResponse = false;
        }

        setMessageProperties(originalEvent.getFlowConstruct(), eventToRoute, route);

        if (logger.isDebugEnabled())
        {
            if (route instanceof OutboundEndpoint)
            {
                logger.debug("Message being sent to: " + ((OutboundEndpoint) route).getEndpointURI());
            }
            logger.debug(eventToRoute.getMessage());
        }

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Request payload: \n"
                             + StringMessageUtils.truncate(muleContext.getTransformationService().getPayloadForLogging(eventToRoute.getMessage()), 100, false));
                if (route instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint) route).getMessageProcessors());
                }
            }
            catch (Exception e)
            {
                logger.trace("Request payload: \n(unable to retrieve payload: " + e.getMessage());
                if (route instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint) route).getMessageProcessors());
                }
            }
        }

        MuleEvent result;
        try
        {
            result = sendRequestEvent(originalEvent, eventToRoute, route, awaitResponse);
        }
        catch (MessagingException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new RoutingException(originalEvent, null, e);
        }

        if (getRouterStatistics() != null)
        {
            if (getRouterStatistics().isEnabled())
            {
                getRouterStatistics().incrementRoutedMessage(route);
            }
        }

        if (result != null && !VoidMuleEvent.getInstance().equals(result))
        {
            MuleMessage resultMessage = result.getMessage();
            if (logger.isTraceEnabled())
            {
                if (resultMessage != null)
                {
                    try
                    {
                        logger.trace("Response payload: \n"
                                     + StringMessageUtils.truncate(muleContext.getTransformationService().getPayloadForLogging(resultMessage),
                                                                   100, false));
                    }
                    catch (Exception e)
                    {
                        logger.trace("Response payload: \n(unable to retrieve payload: " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    protected void setMessageProperties(FlowConstruct service, MuleEvent event, MessageProcessor route)
    {
        MuleMessage message = event.getMessage();
        if (replyTo != null)
        {
            // if replyTo is set we'll probably want the correlationId set as
            // well
            message.setReplyTo(replyTo);
            message.setOutboundProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, service.getName());
            if (logger.isDebugEnabled() && route instanceof OutboundEndpoint)
            {
                logger.debug("Setting replyTo=" + replyTo + " for outbound route: "
                             + ((OutboundEndpoint) route).getEndpointURI());
            }
        }
        if (enableCorrelation != CorrelationMode.NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("CorrelationId is already set to '" + message.getCorrelationId()
                                 + "' , not setting it again");
                }
                return;
            }
            else if (correlationSet)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("CorrelationId is already set to '" + message.getCorrelationId()
                                 + "', but router is configured to overwrite it");
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("No CorrelationId is set on the message, will set a new Id");
                }
            }

            String correlation;
            correlation = service.getMessageInfoMapping().getCorrelationId(event);
            if (logger.isDebugEnabled())
            {
                logger.debug("Extracted correlation Id as: " + correlation);
            }

            if (logger.isDebugEnabled())
            {
                StringBuilder buf = new StringBuilder();
                buf.append("Setting Correlation info on Outbound router");
                if (route instanceof OutboundEndpoint)
                {
                    buf.append(" for endpoint: ").append(((OutboundEndpoint) route).getEndpointURI());
                }
                buf.append(SystemUtils.LINE_SEPARATOR).append("Id=").append(correlation);
                // buf.append(", ").append("Seq=").append(seq);
                // buf.append(", ").append("Group Size=").append(group);
                logger.debug(buf.toString());
            }
            message.setCorrelationId(correlation);
            // message.setCorrelationGroupSize(group);
            // message.setCorrelationSequence(seq);
        }
    }

    @Override
    public List<MessageProcessor> getRoutes()
    {
        return routes;
    }

    /*
     * For spring access
     */
    // TODO Use spring factory bean
    @Deprecated
    public void setMessageProcessors(List<MessageProcessor> routes) throws MuleException
    {
        setRoutes(routes);
    }

    public void setRoutes(List<MessageProcessor> routes) throws MuleException
    {
        this.routes.clear();
        for (MessageProcessor route : routes)
        {
            addRoute(route);
        }
    }

    @Override
    public synchronized void addRoute(MessageProcessor route) throws MuleException
    {
        if (initialised.get())
        {
            if (route instanceof MuleContextAware)
            {
                ((MuleContextAware) route).setMuleContext(muleContext);
            }
            if (route instanceof FlowConstructAware)
            {
                ((FlowConstructAware) route).setFlowConstruct(flowConstruct);
            }
            if (route instanceof Initialisable)
            {
                ((Initialisable) route).initialise();
            }
        }
        if (started.get())
        {
            if (route instanceof Startable)
            {
                ((Startable) route).start();
            }
        }
        routes.add(route);
    }

    @Override
    public synchronized void removeRoute(MessageProcessor route) throws MuleException
    {
        if (started.get())
        {
            if (route instanceof Stoppable)
            {
                ((Stoppable) route).stop();
            }
        }
        if (initialised.get())
        {
            if (route instanceof Disposable)
            {
                ((Disposable) route).dispose();
            }
        }
        routes.remove(route);
    }

    public String getReplyTo()
    {
        return replyTo;
    }

    @Override
    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    public CorrelationMode getEnableCorrelation()
    {
        return enableCorrelation;
    }

    public void setEnableCorrelation(CorrelationMode enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    public void setEnableCorrelationAsString(String enableCorrelation)
    {
        if (enableCorrelation != null)
        {
            if (enableCorrelation.equals("ALWAYS"))
            {
                this.enableCorrelation = CorrelationMode.ALWAYS;
            }
            else if (enableCorrelation.equals("NEVER"))
            {
                this.enableCorrelation = CorrelationMode.NEVER;
            }
            else if (enableCorrelation.equals("IF_NOT_SET"))
            {
                this.enableCorrelation = CorrelationMode.IF_NOT_SET;
            }
            else
            {
                throw new IllegalArgumentException("Value for enableCorrelation not recognised: "
                                                   + enableCorrelation);
            }
        }
    }

    public TransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    @Override
    public void setTransactionConfig(TransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    @Override
    public boolean isDynamicRoutes()
    {
        return false;
    }

    /**
     * @param name the route identifier
     * @return the route or null if the endpoint's Uri is not registered
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    public MessageProcessor getRoute(String name)
    {
        for (MessageProcessor route : routes)
        {
            if (route instanceof OutboundEndpoint)
            {
                OutboundEndpoint endpoint = (OutboundEndpoint) route;
                if (endpoint.getName().equals(name))
                {
                    return endpoint;
                }
            }
        }
        return null;
    }

    public RouterResultsHandler getResultsHandler()
    {
        return resultsHandler;
    }

    public void setResultsHandler(RouterResultsHandler resultsHandler)
    {
        this.resultsHandler = resultsHandler;
    }

    /**
     * Send message event to destination.
     */
    protected MuleEvent sendRequestEvent(MuleEvent originalEvent,
                                         MuleEvent eventToRoute,
                                         MessageProcessor route,
                                         boolean awaitResponse) throws MuleException
    {
        if (route == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("connector operation"), originalEvent, null);
        }

        if (awaitResponse)
        {
            int timeout = eventToRoute.getMessage().getOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
            if (timeout >= 0)
            {
                eventToRoute.setTimeout(timeout);
            }
        }
        if (route instanceof MessageProcessorChain)
        {
            return route.process(eventToRoute);
        }
        else
        {
            return notificationTemplate.execute(route, eventToRoute);
        }
    }

    /**
     * Create a new event to be routed to the target MP
     */
    protected MuleEvent createEventToRoute(MuleEvent routedEvent, MuleMessage message)
    {
        return new DefaultMuleEvent(message, routedEvent, true);
    }

    /**
     * Create a fresh copy of a message.
     */
    protected MuleMessage cloneMessage(MuleMessage message)
    {
        return AbstractRoutingStrategy.cloneMessage(message, muleContext);
    }

    /**
     * Creates a fresh copy of a {@link MuleMessage} ensuring that the payload can be cloned (i.e. is not consumable).
     *
     * @param event The {@link MuleEvent} to clone the message from.
     * @return The fresh copy of the {@link MuleMessage}.
     * @throws MessagingException If the message can't be cloned because it carries a consumable payload.
     */
    protected MuleMessage cloneMessage(MuleEvent event, MuleMessage message) throws MessagingException
    {
        return AbstractRoutingStrategy.cloneMessage(event, message, muleContext);
    }

    /**
     * Propagates a number of internal system properties to handle correlation, session, etc. Note that in and
     * out params can be the same message object when not dealing with replies.
     */
    protected void propagateMagicProperties(MuleMessage in, MuleMessage out)
    {
        AbstractRoutingStrategy.propagateMagicProperties(in, out);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        synchronized (routes)
        {
            super.initialise();
            initialised.set(true);
        }
    }

    @Override
    public void dispose()
    {
        synchronized (routes)
        {
            super.dispose();
            routes = Collections.<MessageProcessor>emptyList();
            initialised.set(false);
        }
    }

    @Override
    public void start() throws MuleException
    {
        synchronized (routes)
        {
            super.start();
            started.set(true);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        synchronized (routes)
        {
            super.stop();
            started.set(false);
        }
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public void setRouterStatistics(RouterStatistics stats)
    {
        this.routerStatistics = stats;
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return routes;
    }
}
