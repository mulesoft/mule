/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.MatchableMessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCatchAllStrategy;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.RouterStatisticsRecorder;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.TransformingMatchable;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractCatchAllStrategy;
import org.mule.util.ObjectUtils;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultOutboundRouterCollection</code> is a container of routers. An
 * DefaultOutboundRouterCollection must have atleast one router. By default the first
 * matching router is used to route an event though it is possible to match on all
 * routers meaning that the message will get sent over all matching routers.
 */

public class DefaultOutboundRouterCollection implements OutboundRouterCollection, MessagingExceptionHandlerAware
{
    /**
     * logger used by this class
     */
    protected final transient Log logger = LogFactory.getLog(getClass());

    protected List<MatchableMessageProcessor> routers = new CopyOnWriteArrayList<MatchableMessageProcessor>();
    protected boolean matchAll = false;
    private OutboundRouterCatchAllStrategy catchAllStrategy;

    protected RouterStatistics statistics = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
    protected MuleContext muleContext;
    private MessagingExceptionHandler messagingExceptionHandler;

    @Override
    public MuleEvent process(final MuleEvent event) throws MessagingException
    {
        MuleMessage message = event.getMessage();
        MuleEvent result;
        boolean matchfound = false;

        for (Iterator<MatchableMessageProcessor> iterator = getRoutes().iterator(); iterator.hasNext();)
        {
            OutboundRouter outboundRouter = (OutboundRouter) iterator.next();

            final MuleMessage outboundRouterMessage;
            // Create copy of message for router 1..n-1 if matchAll="true" or if
            // routers require copy because it may mutate payload before match is
            // chosen
            if (iterator.hasNext()
                && (isMatchAll() || ((outboundRouter instanceof TransformingMatchable) && ((TransformingMatchable) outboundRouter).isTransformBeforeMatch())))
            {
                if (((DefaultMuleMessage) message).isConsumable())
                {
                    throw new MessagingException(CoreMessages.cannotCopyStreamPayload(message.getPayload()
                        .getClass()
                        .getName()), event, this);
                }
                outboundRouterMessage = new DefaultMuleMessage(message.getPayload(), message, muleContext);
            }
            else
            {
                outboundRouterMessage = message;
            }

            try
            {
                if (outboundRouter.isMatch(outboundRouterMessage))
                {
                    matchfound = true;
                    // Manage outbound only transactions here
                    final OutboundRouter router = outboundRouter;

                    result = router.process(event);

                    if (!isMatchAll())
                    {
                        return result;
                    }
                }
            }
            catch (MessagingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RoutingException(event, outboundRouter, e);
            }
        }

        if (!matchfound && getCatchAllStrategy() != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message did not match any routers on: " + event.getFlowConstruct().getName()
                             + " invoking catch all strategy");
            }
            return catchAll(event);
        }
        else if (!matchfound)
        {
            logger.warn("Message did not match any routers on: "
                        + event.getFlowConstruct().getName()
                        + " and there is no catch all strategy configured on this router.  Disposing message "
                        + message);
        }
        return event;
    }

    protected MuleEvent catchAll(MuleEvent event) throws RoutingException
    {
        if (getRouterStatistics().isEnabled())
        {
            getRouterStatistics().incrementCaughtMessage();
        }

        return getCatchAllStrategy().process(event);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        for (MatchableMessageProcessor router : routers)
        {
            if (router instanceof MessagingExceptionHandlerAware)
            {
                ((MessagingExceptionHandlerAware) router).setMessagingExceptionHandler(messagingExceptionHandler);
            }
            if (router instanceof Initialisable)
            {
                ((Initialisable) router).initialise();
            }
        }
    }

    @Override
    public void dispose()
    {
        for (MatchableMessageProcessor router : routers)
        {
            if (router instanceof Disposable)
            {
                ((Disposable) router).dispose();
            }
        }
    }

    // TODO Use spring factory bean
    @Deprecated
    public void setMessageProcessors(List<MatchableMessageProcessor> routers)
    {
        for (MatchableMessageProcessor router : routers)
        {
            addRoute(router);
        }
    }

    @Override
    public void addRoute(MatchableMessageProcessor router)
    {
        if (router instanceof RouterStatisticsRecorder)
        {
            ((RouterStatisticsRecorder) router).setRouterStatistics(getRouterStatistics());
        }
        routers.add(router);
    }

    @Override
    public void removeRoute(MatchableMessageProcessor router)
    {
        routers.remove(router);
    }

    @Override
    public List<MatchableMessageProcessor> getRoutes()
    {
        return routers;
    }

    @Override
    public OutboundRouterCatchAllStrategy getCatchAllStrategy()
    {
        return catchAllStrategy;
    }

    @Override
    public void setCatchAllStrategy(OutboundRouterCatchAllStrategy catchAllStrategy)
    {
        this.catchAllStrategy = catchAllStrategy;
        if (this.catchAllStrategy != null && catchAllStrategy instanceof AbstractCatchAllStrategy)
        {
            ((AbstractCatchAllStrategy) this.catchAllStrategy).setRouterStatistics(statistics);
        }
    }

    @Override
    public boolean isMatchAll()
    {
        return matchAll;
    }

    @Override
    public void setMatchAll(boolean matchAll)
    {
        this.matchAll = matchAll;
    }

    @Override
    public RouterStatistics getRouterStatistics()
    {
        return statistics;
    }

    @Override
    public void setRouterStatistics(RouterStatistics stat)
    {
        this.statistics = stat;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public boolean hasEndpoints()
    {
        for (Iterator<?> iterator = routers.iterator(); iterator.hasNext();)
        {
            OutboundRouter router = (OutboundRouter) iterator.next();
            if (router.getRoutes().size() > 0 || router.isDynamicRoutes())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.messagingExceptionHandler = messagingExceptionHandler;
    }
}
