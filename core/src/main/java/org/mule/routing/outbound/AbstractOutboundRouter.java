/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.api.routing.RoutingException;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.AbstractRouter;
import org.mule.routing.CorrelationMode;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.util.Arrays;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks
 * statistics about message processing through the router.
 */
public abstract class AbstractOutboundRouter extends AbstractRouter implements OutboundRouter
{
    /**
     * These properties are automatically propagated by Mule from inbound to outbound
     */
    protected static List<String> magicProperties = Arrays.asList(
        MuleProperties.MULE_CORRELATION_ID_PROPERTY,
        MuleProperties.MULE_CORRELATION_ID_PROPERTY,
        MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
        MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
        MuleProperties.MULE_SESSION_PROPERTY
    );

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    @SuppressWarnings("unchecked")
    protected List<MessageProcessor> routes = new CopyOnWriteArrayList();

    protected String replyTo = null;

    /**
     * Determines if Mule stamps outgoing message with a correlation ID or not.
     */
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;

    protected TransactionConfig transactionConfig;

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return route(event);
    }
    
    protected abstract MuleEvent route(MuleEvent event) throws MessagingException;

    protected final MuleEvent sendRequest(final MuleEvent routedEvent, final MuleMessage message, final MessageProcessor route,
                                          boolean awaitResponse)
            throws MuleException
    {
        if (awaitResponse && replyTo != null)
        {
            logger.debug("event was dispatched synchronously, but there is a ReplyTo route set, so using asynchronous dispatch");
            awaitResponse = false;
        }

        setMessageProperties(routedEvent.getSession().getFlowConstruct(), message, route);

        if (logger.isDebugEnabled())
        {
            if (route instanceof OutboundEndpoint)
            {
                logger.debug("Message being sent to: " + ((OutboundEndpoint)route).getEndpointURI());
            }
            logger.debug(message);
        }

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Request payload: \n"
                    + StringMessageUtils.truncate(message.getPayloadAsString(), 100, false));
                if (route instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint)route).getTransformers());
                }
            }
            catch (Exception e)
            {
                logger.trace("Request payload: \n(unable to retrieve payload: " + e.getMessage());
                if (route instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint)route).getTransformers());
                }
            }
        }

        MuleEvent result;
        try
        {
            result = sendRequestEvent(routedEvent, message, route, awaitResponse);
        }
        catch (MessagingException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new RoutingException(routedEvent, null, e);
        }
        
        if (getRouterStatistics() != null)
        {
            if (getRouterStatistics().isEnabled())
            {
                getRouterStatistics().incrementRoutedMessage(route);
            }
        }

        if (result != null)
        {
            MuleMessage resultMessage = result.getMessage();
            if (logger.isTraceEnabled())
            {
                if (resultMessage != null)
                {
                    try
                    {
                        logger.trace("Response payload: \n"
                            + StringMessageUtils.truncate(resultMessage.getPayloadAsString(), 100, false));
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

    protected void setMessageProperties(FlowConstruct service, MuleMessage message, MessageProcessor route)
    {
        if (replyTo != null)
        {
            // if replyTo is set we'll probably want the correlationId set as
            // well
            message.setReplyTo(replyTo);
            message.setOutboundProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, service.getName());
            if (logger.isDebugEnabled() && route instanceof OutboundEndpoint)
            {
                logger.debug("Setting replyTo=" + replyTo + " for outbound route: "
                        + ((OutboundEndpoint)route).getEndpointURI());
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
            correlation = service.getMessageInfoMapping().getCorrelationId(message);
            if (logger.isDebugEnabled())
            {
                logger.debug("Extracted correlation Id as: " + correlation);
            }

            if (logger.isDebugEnabled())
            {
                StringBuffer buf = new StringBuffer();
                buf.append("Setting Correlation info on Outbound router");
                if (route instanceof OutboundEndpoint)
                {
                    buf.append(" for endpoint: ").append(
                        ((OutboundEndpoint)route).getEndpointURI());
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

    public List<MessageProcessor> getRoutes()
    {
        return routes;
    }

    /*
     * For spring access
     */
    public void setEndpoints(List<MessageProcessor> routes)
    {
        setRoutes(routes);
    }

    public void setRoutes(List<MessageProcessor> routes)
    {
        this.routes.clear();
        for (MessageProcessor route : routes)
        {
            addRoute(route);
        }
    }

    public void addRoute(MessageProcessor route)
    {
        routes.add(route);
    }

    public boolean removeRoute(MessageProcessor route)
    {
        return routes.remove(route);
    }

    public String getReplyTo()
    {
        return replyTo;
    }

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

    public void setTransactionConfig(TransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    public boolean isDynamicRoutes()
    {
        return false;
    }

    /**
     * @param name the route identifier
     * @return the route or null if the endpoint's Uri is not registered
     * @see org.mule.api.routing.InboundRouterCollection
     */
    public MessageProcessor getRoute(String name)
    {
        for (MessageProcessor route  : routes)
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
    
    public boolean isRequiresNewMessage()
    {
        return false;
    }

    /**
     *  Send message event to destination.
     */
    protected MuleEvent sendRequestEvent(MuleEvent routedEvent, MuleMessage message, 
        MessageProcessor route, boolean awaitResponse) throws MuleException
    {
        if (route == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("Outbound Endpoint"), routedEvent, null);
        }

        ImmutableEndpoint endpoint = (route instanceof ImmutableEndpoint) ? (ImmutableEndpoint)route : routedEvent.getEndpoint();
        MuleEvent event = new DefaultMuleEvent(message, endpoint, routedEvent.getSession());

        if (awaitResponse)
        {
            int timeout = message.getOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
            if (timeout >= 0)
            {
                event.setTimeout(timeout);
            }
        }

        return route.process(event);
    }

    /**
     * Propagates a number of internal system properties to handle correlation, session, etc. Note that
     * in and out params can be the same message object when not dealing with replies.
     * @see #magicProperties
     */
    protected void propagateMagicProperties(MuleMessage in, MuleMessage out)
    {
        for (String name : magicProperties)
        {
            Object value = in.getInboundProperty(name);
            if (value != null)
            {
                out.setOutboundProperty(name, value);
            }
        }
    }

}
