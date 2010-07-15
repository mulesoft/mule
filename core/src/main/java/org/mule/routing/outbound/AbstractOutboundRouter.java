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
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.api.routing.RoutingException;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.AbstractRouter;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

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
    public static final int ENABLE_CORRELATION_IF_NOT_SET = 0;
    public static final int ENABLE_CORRELATION_ALWAYS = 1;
    public static final int ENABLE_CORRELATION_NEVER = 2;
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    @SuppressWarnings("unchecked")
    protected List<MessageProcessor> targets = new CopyOnWriteArrayList();

    protected String replyTo = null;

    /**
     * Determines if Mule stamps outgoing message with a correlation ID or not.
     */
    protected int enableCorrelation = ENABLE_CORRELATION_IF_NOT_SET;

    protected MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();

    protected TransactionConfig transactionConfig;

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return route(event);
    }
    
    protected abstract MuleEvent route(MuleEvent event) throws MessagingException;

    protected final MuleEvent sendRequest(final MuleEvent routedEvent, final MuleMessage message, final MessageProcessor target,
                                          boolean awaitResponse)
            throws MuleException
    {
        if (awaitResponse && replyTo != null)
        {
            logger.debug("event was dispatched synchronously, but there is a ReplyTo endpoint set, so using asynchronous dispatch");
            awaitResponse = false;
        }

        setMessageProperties(routedEvent.getSession().getFlowConstruct(), message, target);

        if (logger.isDebugEnabled())
        {
            if (target instanceof OutboundEndpoint)
            {
                logger.debug("Message being sent to: " + ((OutboundEndpoint)target).getEndpointURI());
            }
            logger.debug(message);
        }

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Request payload: \n"
                    + StringMessageUtils.truncate(message.getPayloadAsString(), 100, false));
                if (target instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint)target).getTransformers());
                }
            }
            catch (Exception e)
            {
                logger.trace("Request payload: \n(unable to retrieve payload: " + e.getMessage());
                if (target instanceof OutboundEndpoint)
                {
                    logger.trace("outbound transformer is: " + ((OutboundEndpoint)target).getTransformers());
                }
            }
        }

        MuleEvent result;
        try
        {
            result = sendRequestEvent(routedEvent, message, target, awaitResponse);
        }
        catch (MessagingException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new RoutingException(message, null, e);
        }
        
        if (getRouterStatistics() != null)
        {
            if (getRouterStatistics().isEnabled())
            {
                getRouterStatistics().incrementRoutedMessage(target);
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

    protected void setMessageProperties(FlowConstruct service, MuleMessage message, MessageProcessor endpoint)
    {
        if (replyTo != null)
        {
            // if replyTo is set we'll probably want the correlationId set as
            // well
            message.setReplyTo(replyTo);
            message.setProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, service.getName(), PropertyScope.OUTBOUND);
            if (logger.isDebugEnabled() && endpoint instanceof OutboundEndpoint)
            {
                logger.debug("Setting replyTo=" + replyTo + " for outbound endpoint: "
                        + ((OutboundEndpoint)endpoint).getEndpointURI());
            }
        }
        if (enableCorrelation != ENABLE_CORRELATION_NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
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
            correlation = messageInfoMapping.getCorrelationId(message);
            if (logger.isDebugEnabled())
            {
                logger.debug("Extracted correlation Id as: " + correlation);
            }

            if (logger.isDebugEnabled())
            {
                StringBuffer buf = new StringBuffer();
                buf.append("Setting Correlation info on Outbound router");
                if (endpoint instanceof OutboundEndpoint)
                {
                    buf.append(" for endpoint: ").append(
                        ((OutboundEndpoint)endpoint).getEndpointURI());
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

    public List<MessageProcessor> getTargets()
    {
        return targets;
    }

    /*
     * For spring access
     */
    public void setEndpoints(List<MessageProcessor> endpoints)
    {
        setTargets(endpoints);
    }

    public void setTargets(List<MessageProcessor> endpoints)
    {
        this.targets.clear();
        for (MessageProcessor target : endpoints)
        {
            addTarget(target);
        }
    }

    public void addTarget(MessageProcessor endpoint)
    {
        targets.add(endpoint);
    }

    public boolean removeTarget(MessageProcessor endpoint)
    {
        return targets.remove(endpoint);
    }

    public String getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    public int getEnableCorrelation()
    {
        return enableCorrelation;
    }

    public void setEnableCorrelation(int enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    public void setEnableCorrelationAsString(String enableCorrelation)
    {
        if (enableCorrelation != null)
        {
            if (enableCorrelation.equals("ALWAYS"))
            {
                this.enableCorrelation = ENABLE_CORRELATION_ALWAYS;
            }
            else if (enableCorrelation.equals("NEVER"))
            {
                this.enableCorrelation = ENABLE_CORRELATION_NEVER;
            }
            else if (enableCorrelation.equals("IF_NOT_SET"))
            {
                this.enableCorrelation = ENABLE_CORRELATION_IF_NOT_SET;
            }
            else
            {
                throw new IllegalArgumentException("Value for enableCorrelation not recognised: "
                        + enableCorrelation);
            }
        }
    }

    public MessageInfoMapping getMessageInfoMapping()
    {
        return messageInfoMapping;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

    public TransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public void setTransactionConfig(TransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    public boolean isDynamicTargets()
    {
        return false;
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see org.mule.api.routing.InboundRouterCollection
     */
    public MessageProcessor getTarget(String name)
    {
        for (MessageProcessor target  : targets)
        {
            if (target instanceof OutboundEndpoint)
            {
                OutboundEndpoint endpoint = (OutboundEndpoint) target;
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
    protected MuleEvent sendRequestEvent(
        MuleEvent routedEvent, MuleMessage message, MessageProcessor target, boolean awaitResponse)
            throws MuleException
    {
        if (target == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("Outbound Endpoint"), message, null);
        }

        ImmutableEndpoint endpoint = (target instanceof ImmutableEndpoint) ? (ImmutableEndpoint)target : routedEvent.getEndpoint();
        MuleEvent event = new DefaultMuleEvent(message, endpoint, routedEvent.getSession());

        if (awaitResponse)
        {
            int timeout = message.getIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
            if (timeout >= 0)
            {
                event.setTimeout(timeout);
            }
        }

        return target.process(event);
    }

}
