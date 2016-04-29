/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.util.ClassUtils.isConsumable;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.StringMessageUtils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Abstract routing strategy with utility methods to be reused by routing strategies
 */
public abstract class AbstractRoutingStrategy implements RoutingStrategy
{

    /**
     * These properties are automatically propagated by Mule from inbound to outbound
     */
    protected static List<String> magicProperties = Arrays.asList(
            MuleProperties.MULE_CORRELATION_ID_PROPERTY, MuleProperties.MULE_CORRELATION_ID_PROPERTY,
            MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
            MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, MuleProperties.MULE_SESSION_PROPERTY);

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AbstractRoutingStrategy.class);

    private final MuleContext muleContext;

    public AbstractRoutingStrategy(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }


    /**
     * Send message event to destination.
     *
     * Creates a new event that will be used to process the route.
     *
     * @param routedEvent event to route
     * @param message message to route
     * @param route message processor to be executed
     * @param awaitResponse if the
     * @return
     * @throws MuleException
     */
    protected final MuleEvent sendRequest(final MuleEvent routedEvent,
                                          final MuleMessage message,
                                          final MessageProcessor route,
                                          boolean awaitResponse) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            if (route instanceof OutboundEndpoint)
            {
                logger.debug("Message being sent to: " + ((OutboundEndpoint) route).getEndpointURI());
            }
            logger.debug(message);
        }

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Request payload: \n"
                             + StringMessageUtils.truncate(muleContext.getTransformationService().getPayloadForLogging(message), 100, false));
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
                                     + StringMessageUtils.truncate(muleContext.getTransformationService().getPayloadForLogging(resultMessage), 100,
                                                                   false));
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

    private MuleEvent sendRequestEvent(MuleEvent routedEvent,
                                         MuleMessage message,
                                         MessageProcessor route,
                                         boolean awaitResponse) throws MuleException
    {
        if (route == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("route"), routedEvent, null);
        }

        MuleEvent event = createEventToRoute(routedEvent, message, route);

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
     * Create a new event to be routed to the target MP
     */
    protected MuleEvent createEventToRoute(MuleEvent routedEvent, MuleMessage message, MessageProcessor route)
    {
        return new DefaultMuleEvent(message, routedEvent, true);
    }

    /**
     * Create a fresh copy of a message.
     */
    public static MuleMessage cloneMessage(MuleMessage message, MuleContext muleContext)
    {
        return new DefaultMuleMessage(message.getPayload(), message, muleContext);
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

    /**
     * Propagates a number of internal system properties to handle correlation, session, etc. Note that in and
     * out params can be the same message object when not dealing with replies.
     *
     * @see #magicProperties
     *
     * This method is mostly used by routers that dispatch the same message to several routes
     */
    public static void propagateMagicProperties(MuleMessage in, MuleMessage out)
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

    /**
     * Validates that the payload is not consumable so it can be copied.
     *
     * If validation fails then throws a MessagingException
     *
     * @param event
     * @param message
     * @throws MessagingException
     */
    public static void validateMessageIsNotConsumable(MuleEvent event, MuleMessage message) throws MessagingException
    {
        if (isConsumable(message.getPayload().getClass()))
        {
            throw new MessagingException(
                    CoreMessages.cannotCopyStreamPayload(message.getPayload().getClass().getName()),
                    event);
        }
    }

    public static MuleMessage cloneMessage(MuleEvent event, MuleMessage message, MuleContext muleContext) throws MessagingException
    {
        assertNonConsumableMessage(event, message);
        return cloneMessage(message, muleContext);
    }

    /**
     * Asserts that the {@link MuleMessage} in the {@link MuleEvent} doesn't carry a consumable payload. This method
     * is useful for routers which need to clone the message before dispatching the message to multiple routes.
     *
     * @param event The {@link MuleEvent}.
     * @param event The {@link MuleMessage} whose payload is to be verified.
     * @throws MessagingException If the payload of the message is consumable.
     */
    protected static void assertNonConsumableMessage(MuleEvent event, MuleMessage message) throws MessagingException
    {
        if (isConsumable(message.getPayload().getClass()))
        {
            throw new MessagingException(CoreMessages.cannotCopyStreamPayload(message.getPayload().getClass().getName()), event);
        }
    }
}
