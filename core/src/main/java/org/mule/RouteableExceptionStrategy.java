/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.routing.OutboundRouter;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.message.DefaultExceptionPayload;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.NullPayload;
import org.mule.util.ObjectUtils;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * <code>RouteableExceptionStrategy</code> allows transforming and routing exceptions
 * to outbound routers. This exception strategy does not take into account any
 * defined endpoints in its instance variable.
 *
 * @author estebanroblesluna
 * @since 2.2.6
 */
public class RouteableExceptionStrategy extends AbstractMessagingExceptionStrategy implements FlowConstructAware, Lifecycle
{

    private OutboundRouter router;

    private boolean stopFurtherProcessing = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleEvent handleException(Exception e, MuleEvent event)
    {
        int currentRootExceptionHashCode = 0;
        int originalRootExceptionHashCode = 0;
        MuleMessage msg = null;

        StringBuffer logInfo = new StringBuffer();

        try
        {
            logInfo.append("****++******Alternate Exception Strategy******++*******\n");
            logInfo.append("Current Thread = " + Thread.currentThread().toString() + "\n");

            if (event != null && event.getFlowConstruct() != null)
            {
                String serviceName = event.getFlowConstruct().getName();
                logInfo.append("serviceName = " + serviceName + "\n");

                int eventHashCode = event.hashCode();
                logInfo.append("eventHashCode = " + eventHashCode + "\n");
            }

            if (event != null && event.isStopFurtherProcessing())
            {
                logInfo.append("MuleEvent stop further processing has been set, This is probably the same exception being routed again. no Exception routing will be performed.\n"
                            + e
                            + "\n");
                event.getMessage().setPayload(NullPayload.getInstance());
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                return event;
            }

            Throwable root = ExceptionUtils.getRootCause(e);
            currentRootExceptionHashCode = root == null ? -1 : root.hashCode();

            msg = event == null ? null : event.getMessage();

            if (msg != null)
            {
                int msgHashCode = msg.hashCode();
                logInfo.append("msgHashCode = " + msgHashCode + "\n");

                if (msg.getExceptionPayload() != null)
                {
                    Throwable t = msg.getExceptionPayload().getRootException();
                    if (t != null && t.hashCode() == currentRootExceptionHashCode)
                    {
                        logInfo.append("*#*#*#*#*\n");
                        logInfo.append("This error has already been handeled, returning without doing anything: "
                                    + e.getMessage()
                                    + "\n");
                        logInfo.append("*#*#*#*#*\n");
                        originalRootExceptionHashCode = currentRootExceptionHashCode;
                        event.getMessage().setPayload(NullPayload.getInstance());
                        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                        return event;
                    }
                }

                originalRootExceptionHashCode = msg.getIntProperty("RootExceptionHashCode", 0);

                logInfo.append("Original RootExceptionHashCode: " + originalRootExceptionHashCode + "\n");
                logInfo.append("Current  RootExceptionHashCode: " + currentRootExceptionHashCode + "\n");

                if (originalRootExceptionHashCode == 0)
                {
                    msg.setIntProperty("RootExceptionHashCode", currentRootExceptionHashCode);
                    originalRootExceptionHashCode = currentRootExceptionHashCode;
                }
                else if (originalRootExceptionHashCode == currentRootExceptionHashCode)
                {
                    logInfo.append("*#*#*#*#*\n");
                    logInfo.append("This error has already been handeled, returning without doing anything: "
                                + e.getMessage()
                                + "\n");
                    logInfo.append("*#*#*#*#*\n");
                    event.getMessage().setPayload(NullPayload.getInstance());
                    event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                    return event;
                }
                else
                {
                    msg.setIntProperty("RootExceptionHashCode", currentRootExceptionHashCode);
                }
            }

            logInfo.append(e.getMessage());

            StackTraceElement[] st = e.getStackTrace();
            for (int i = 0; i < st.length; i++)
            {
                if (st[i].getClassName().equals("org.mule.AlternateExceptionStrategy"))
                {
                    logger.warn("*#*#*#*#*\n"
                        + "Recursive error in AlternateExceptionStrategy "
                        + e
                        + "\n"
                        + "*#*#*#*#*");
                    event.getMessage().setPayload(NullPayload.getInstance());
                    event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                    return event;
                }
                logger.debug(st[i].toString());
            }
            return super.handleException(e, event);
        }
        finally
        {
            if (event != null && this.stopFurtherProcessing) event.setStopFurtherProcessing(true);

            if (msg != null && currentRootExceptionHashCode != 0
                && currentRootExceptionHashCode != originalRootExceptionHashCode)
                msg.setIntProperty("RootExceptionHashCode", currentRootExceptionHashCode);

            logInfo.append("****__******Alternate Exception Strategy******__*******\n");
            logger.debug(logInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void handleMessagingException(MuleMessage message, Throwable t)
    {
        defaultHandler(message, t);
        routeException(getMessageFromContext(message), (ImmutableEndpoint) null, t);
    }

    public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable t)
    {
        defaultHandler(message, t);
        routeException(getMessageFromContext(message), endpoint, t);
    }

    /**
     * {@inheritDoc}
     */
    public void handleLifecycleException(Object component, Throwable t)
    {
        logger.error("The object that failed is: \n" + ObjectUtils.toString(component, "null"));
        handleStandardException(t);
    }

    /**
     * {@inheritDoc}
     */
    public void handleStandardException(Throwable t)
    {
        handleTransaction(t);
        if (RequestContext.getEvent() != null)
        {
            handleMessagingException(RequestContext.getEvent().getMessage(), t);
        }
        else
        {
            logger.info("There is no current event available, routing Null message with the exception");
            handleMessagingException(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), t);
        }
    }

    protected void defaultHandler(MuleMessage message, Throwable t)
    {
        if (RequestContext.getEvent() != null && RequestContext.getEvent().getMessage() != null)
        {
            RequestContext.getEvent().getMessage().setExceptionPayload(new DefaultExceptionPayload(t));
        }

        if (message != null) message.setExceptionPayload(new DefaultExceptionPayload(t));
    }

    protected MuleMessage getMessageFromContext(MuleMessage message)
    {
        if (RequestContext.getEvent() != null)
        {
            return RequestContext.getEvent().getMessage();
        }
        else if (message != null)
        {
            return message;
        }
        else
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }
    }

    protected void routeException(MuleMessage msg, ImmutableEndpoint failedEndpoint, Throwable t)
    {
        MuleMessage contextMsg = null;
        MuleEvent exceptionEvent = RequestContext.getEvent();
        contextMsg = exceptionEvent == null ? msg : exceptionEvent.getMessage();

        if (contextMsg == null)
        {
            contextMsg = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            contextMsg.setExceptionPayload(new DefaultExceptionPayload(t));
        }

        if (exceptionEvent == null)
        {
            exceptionEvent = new DefaultMuleEvent(contextMsg, failedEndpoint, new DefaultMuleSession(muleContext));
        }

        // copy the message
        DefaultMuleMessage messageCopy = new DefaultMuleMessage(contextMsg.getPayload(), contextMsg, muleContext);

        // route the message
        try
        {
            router.process(exceptionEvent);
        }
        catch (MuleException e)
        {
            logFatal(exceptionEvent, e);
        }
    }

    public OutboundRouter getRouter()
    {
        return router;
    }

    public void setRouter(OutboundRouter router)
    {
        this.router = router;
    }

    public boolean isStopFurtherProcessing()
    {
        return stopFurtherProcessing;
    }

    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        this.stopFurtherProcessing = stopFurtherProcessing;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        if (router instanceof FlowConstructAware)
        {
            router.setFlowConstruct(flowConstruct);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        if (router instanceof MuleContextAware)
        {
            router.setMuleContext(context);
        }
    }

    @Override
    protected void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        super.doInitialise(muleContext);
        if (router instanceof Initialisable)
        {
            router.initialise();
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (router instanceof Disposable)
        {
            router.dispose();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (router instanceof Stoppable)
        {
            router.stop();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (router instanceof Stoppable)
        {
            router.stop();
        }
    }
}
