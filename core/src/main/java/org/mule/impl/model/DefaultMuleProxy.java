/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.RequestContext;
import org.mule.impl.message.ExceptionPayload;
import org.mule.management.stats.ComponentStatistics;
import org.mule.providers.AbstractConnector;
import org.mule.providers.NullPayload;
import org.mule.providers.ReplyToHandler;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.model.UMOModel;
import org.mule.util.queue.QueueSession;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleProxy</code> is a proxy to an UMO. It can be executed in its own thread.
 */
public class DefaultMuleProxy implements MuleProxy
{
    /** logger used by this class */
    private static Log logger = LogFactory.getLog(DefaultMuleProxy.class);

    /** Holds the current event being processed */
    private UMOEvent event;

    /** Holds the actual UMO */
    private UMOLifecycleAdapter umo;

    /** holds the UMO descriptor */
    private UMOComponent component;

    /** Determines if the proxy is suspended */
    private boolean suspended = true;

    private ComponentStatistics stat = null;

    private QueueSession queueSession = null;

    protected UMOManagementContext managementContext;
    
    /**
     * Constructs a Proxy using the UMO's AbstractMessageDispatcher and the UMO
     * itself
     *
     * @param component  the underlying object that with receive events
     * @param descriptor the UMOComponent descriptor associated with the component
     */
    public DefaultMuleProxy(Object pojoService, UMOComponent component, UMOManagementContext managementContext)
            throws UMOException
    {
        //this.pojoService = pojoService;
        this.component = component;
        this.managementContext = managementContext;

        UMOModel model = component.getModel();
        UMOEntryPointResolverSet resolver = model.getEntryPointResolverSet();
        umo = model.getLifecycleAdapterFactory().create(pojoService, component, resolver);
    }

    public void start() throws UMOException
    {
        checkDisposed();
        if (!umo.isStarted())
        {
            try
            {
                umo.start();
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStart("Component '" + component.getName() + "'"), e);
            }
        }

    }

    public boolean isStarted()
    {
        return umo.isStarted();
    }

    public void stop() throws UMOException
    {
        checkDisposed();
        if (umo.isStarted())
        {
            try
            {
                umo.stop();
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStop("Component '" + component.getName() + "'"), e);
            }
        }
    }

    public void dispose()
    {
        checkDisposed();
    }

    private void checkDisposed()
    {
        if (umo.isDisposed())
        {
            throw new IllegalStateException("Component has already been disposed of");
        }
    }

    /**
     * Sets the current event being processed
     *
     * @param event the event being processed
     */
    public void onEvent(QueueSession session, UMOEvent event)
    {
        this.queueSession = session;
        this.event = event;
    }

    public ComponentStatistics getStatistics()
    {
        return stat;
    }

    public void setStatistics(ComponentStatistics stat)
    {
        this.stat = stat;
    }

    /**
     * Makes a synchronous call on the UMO
     *
     * @param event the event to pass to the UMO
     * @return the return event from the UMO
     * @throws UMOException if the call fails
     */
    public Object onCall(UMOEvent event) throws UMOException
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("MuleProxy: sync call for Mule UMO " + component.getName());
        }

        UMOMessage returnMessage = null;
        try
        {
            if (event.getEndpoint().canReceive())
            {
                event = OptimizedRequestContext.unsafeSetEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), event.getEndpoint());

                // stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }

                if (component.getName().startsWith("_xfireServiceComponent") ||
                    component.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    event.getMessage().setBooleanProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY, true);
                }
                returnMessage = umo.intercept(null);
                
                if (component.getName().startsWith("_xfireServiceComponent") ||
                    component.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    returnMessage.removeProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY);
                }

                // stats
                if (stat.isEnabled())
                {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // this is the request event
                event = RequestContext.getEvent();
                if (event.isStopFurtherProcessing())
                {
                    logger.debug("Event stop further processing has been set, no outbound routing will be performed.");
                }
                if (returnMessage != null && !event.isStopFurtherProcessing())
                {
                    if (component.getOutboundRouter().hasEndpoints())
                    {
                        UMOMessage outboundReturnMessage = component.getOutboundRouter().route(
                                returnMessage, event.getSession(), event.isSynchronous());
                        if (outboundReturnMessage != null)
                        {
                            returnMessage = outboundReturnMessage;
                        }
                    }
                    else
                    {
                        logger.debug("Outbound router on component '" + component.getName()
                                + "' doesn't have any endpoints configured.");
                    }
                }

                // Process Response Router
                // TODO Alan C. - responseRouter is initialized to empty (no endpoints) in Mule 2.x, this line can be part of a solution
                //if (returnMessage != null && component.getResponseRouter() != null && !component.getResponseRouter().getEndpoints().isEmpty())
                if (returnMessage != null && component.getResponseRouter() != null)
                {
                    logger.debug("Waiting for response router message");
                    returnMessage = component.getResponseRouter().getResponse(returnMessage);
                }

                // process replyTo if there is one
                if (returnMessage != null && replyToHandler != null)
                {
                    String requestor = (String) returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(component.getName())) || requestor == null)
                    {
                        replyToHandler.processReplyTo(event, returnMessage, replyTo);
                    }
                }

            }
            else
            {
                returnMessage = event.getSession().sendEvent(event);
                processReplyTo(returnMessage);
            }

            // stats
            if (stat.isEnabled())
            {
                stat.incSentEventSync();
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MessagingException)
            {
                handleException(e);
            }
            else
            {
                handleException(
                    new MessagingException(
                        CoreMessages.eventProcessingFailedFor(component.getName()), 
                        event.getMessage(), e));
            }

            if (returnMessage == null)
            {
                returnMessage = new MuleMessage(NullPayload.getInstance(), (Map) null);
            }
            UMOExceptionPayload exceptionPayload = RequestContext.getExceptionPayload();
            if (exceptionPayload == null)
            {
                exceptionPayload = new ExceptionPayload(e);
            }
            returnMessage.setExceptionPayload(exceptionPayload);
        }
        return returnMessage;
    }

    /**
     * When an exception occurs this method can be called to invoke the configured
     * UMOExceptionStrategy on the UMO
     *
     * @param exception If the UMOExceptionStrategy implementation fails
     */
    public void handleException(Exception exception)
    {
        component.getExceptionListener().exceptionThrown(exception);
    }

    public String toString()
    {
        return "proxy for: " + component.toString();
    }

    /**
     * Determines if the proxy is suspended
     *
     * @return true if the proxy (and the UMO) are suspended
     */
    public boolean isSuspended()
    {
        return suspended;
    }

    /** Controls the suspension of the UMO event processing */
    public void suspend()
    {
        suspended = true;
    }

    /** Triggers the UMO to resume processing of events if it is suspended */
    public void resume()
    {
        suspended = false;
    }

    protected ReplyToHandler getReplyToHandler(UMOMessage message, UMOImmutableEndpoint endpoint)
    {
        Object replyTo = message.getReplyTo();
        ReplyToHandler replyToHandler = null;
        if (replyTo != null)
        {
            replyToHandler = ((AbstractConnector) endpoint.getConnector()).getReplyToHandler();
            // Use the response transformer for the event if one is set
            if (endpoint.getResponseTransformers() != null)
            {
                replyToHandler.setTransformers(endpoint.getResponseTransformers());
            }
        }
        return replyToHandler;
    }

    private void processReplyTo(UMOMessage returnMessage) throws UMOException
    {
        if (returnMessage != null && returnMessage.getReplyTo() != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("sending reply to: " + returnMessage.getReplyTo());
            }

            // get the endpointUri for this uri
            UMOImmutableEndpoint endpoint = 
                managementContext.getRegistry().lookupOutboundEndpoint(
                    returnMessage.getReplyTo().toString(), managementContext);
            // make sure remove the replyTo property as not cause a a forever
            // replyto loop
            returnMessage.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);

            // Create the replyTo event asynchronous
            UMOEvent replyToEvent = new MuleEvent(returnMessage, endpoint, event.getSession(), false);

            // queue the event
            onEvent(queueSession, replyToEvent);

            if (logger.isDebugEnabled())
            {
                logger.debug("reply to sent: " + returnMessage.getReplyTo());
            }

            if (stat.isEnabled())
            {
                stat.incSentReplyToEvent();
            }
        }
    }

    public void run()
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("MuleProxy: async onEvent for Mule UMO " + component.getName());
        }

        try
        {
            if (event.getEndpoint().canReceive())
            {
                // dispatch the next receiver
                event = OptimizedRequestContext.criticalSetEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), event.getEndpoint());

                // do stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }
                
                if (component.getName().startsWith("_xfireServiceComponent") ||
                    component.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    event.getMessage().setBooleanProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY, true);
                }
                UMOMessage result = umo.intercept(null);
                
                if (component.getName().startsWith("_xfireServiceComponent") ||
                    component.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    result.removeProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY);
                }
                
                if (stat.isEnabled())
                {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // processResponse(result, replyTo, replyToHandler);
                event = RequestContext.getEvent();
                if (result != null && !event.isStopFurtherProcessing())
                {
                    component.getOutboundRouter().route(result, event.getSession(), event.isSynchronous());
                }

                // process replyTo if there is one
                if (result != null && replyToHandler != null)
                {
                    String requestor = (String) result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(component.getName())) || requestor == null)
                    {
                        replyToHandler.processReplyTo(event, result, replyTo);
                    }
                }
            }
            else
            {
                event.getEndpoint().dispatch(event);
            }

            if (stat.isEnabled())
            {
                stat.incSentEventASync();
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MessagingException)
            {
                handleException(e);
            }
            else
            {
                handleException(
                    new MessagingException(
                        CoreMessages.eventProcessingFailedFor(component.getName()), 
                        event.getMessage(), e));
            }
        }
    }

    public void release()
    {
        // nothing to do
    }
}
