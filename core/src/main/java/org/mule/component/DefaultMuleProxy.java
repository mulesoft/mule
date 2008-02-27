/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.LifecycleAdapter;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.Model;
import org.mule.api.model.ModelException;
import org.mule.api.model.MuleProxy;
import org.mule.api.service.Service;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.ServiceStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.AbstractConnector;
import org.mule.transport.NullPayload;
import org.mule.util.queue.QueueSession;

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
    private MuleEvent event;

    /** Holds the actual UMO */
    private LifecycleAdapter umo;

    /** holds the UMO descriptor */
    private Service service;

    /** Determines if the proxy is suspended */
    private boolean suspended = true;

    private ServiceStatistics stat = null;

    private QueueSession queueSession = null;

    protected MuleContext muleContext;
    
    /**
     * Constructs a Proxy using the UMO's AbstractMessageDispatcher and the UMO
     * itself
     */
    public DefaultMuleProxy(Object pojoService, Service service, MuleContext muleContext)
            throws MuleException
    {
        //this.pojoService = pojoService;
        this.service = service;
        this.muleContext = muleContext;

        Model model = service.getModel();
        EntryPointResolverSet resolver = model.getEntryPointResolverSet();
        umo = model.getLifecycleAdapterFactory().create(pojoService, service, resolver);
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        checkDisposed();
        if (!umo.isStarted())
        {
            try
            {
                return umo.start();
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStart("Service '" + service.getName() + "'"), e);
            }
        }
        return LifecycleTransitionResult.OK;
    }

    public boolean isStarted()
    {
        return umo.isStarted();
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        checkDisposed();
        if (umo.isStarted())
        {
            try
            {
                return umo.stop();
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStop("Service '" + service.getName() + "'"), e);
            }
        }
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {
        checkDisposed();
    }

    private void checkDisposed()
    {
        if (umo.isDisposed())
        {
            throw new IllegalStateException("Service has already been disposed of");
        }
    }

    /**
     * Sets the current event being processed
     *
     * @param event the event being processed
     */
    public void onEvent(QueueSession session, MuleEvent event)
    {
        this.queueSession = session;
        this.event = event;
    }

    public ServiceStatistics getStatistics()
    {
        return stat;
    }

    public void setStatistics(ServiceStatistics stat)
    {
        this.stat = stat;
    }

    /**
     * Makes a synchronous call on the UMO
     *
     * @param event the event to pass to the UMO
     * @return the return event from the UMO
     * @throws MuleException if the call fails
     */
    public Object onCall(MuleEvent event) throws MuleException
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("MuleProxy: sync call for Mule UMO " + service.getName());
        }

        MuleMessage returnMessage = null;
        try
        {
            if (event.getEndpoint() instanceof InboundEndpoint)
            {
                InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();
                event = OptimizedRequestContext.unsafeSetEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), endpoint);

                // stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }

                if (service.getName().startsWith("_xfireServiceComponent") ||
                    service.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    event.getMessage().setBooleanProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY, true);
                }
                returnMessage = umo.intercept(null);
                
                if (service.getName().startsWith("_xfireServiceComponent") ||
                    service.getName().startsWith("_axisServiceComponent"))
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
               // event = RequestContext.getEvent();
                if (event.isStopFurtherProcessing())
                {
                    logger.debug("MuleEvent stop further processing has been set, no outbound routing will be performed.");
                }
                if (returnMessage != null && !event.isStopFurtherProcessing())
                {
                    if (service.getOutboundRouter().hasEndpoints())
                    {
                        MuleMessage outboundReturnMessage = service.getOutboundRouter().route(
                                returnMessage, event.getSession(), event.isSynchronous());
                        if (outboundReturnMessage != null)
                        {
                            returnMessage = outboundReturnMessage;
                        }
                    }
                    else
                    {
                        logger.debug("Outbound router on service '" + service.getName()
                                + "' doesn't have any endpoints configured.");
                    }
                }

                // Process Response Router
                // TODO Alan C. - responseRouter is initialized to empty (no endpoints) in Mule 2.x, this line can be part of a solution
                //if (returnMessage != null && service.getResponseRouter() != null && !service.getResponseRouter().getEndpoints().isEmpty())
                if (returnMessage != null && service.getResponseRouter() != null)
                {
                    logger.debug("Waiting for response router message");
                    returnMessage = service.getResponseRouter().getResponse(returnMessage);
                }

                // process replyTo if there is one
                if (returnMessage != null && replyToHandler != null)
                {
                    String requestor = (String) returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
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
                        CoreMessages.eventProcessingFailedFor(service.getName()), 
                        event.getMessage(), e));
            }

            if (returnMessage == null)
            {
                // important that we pull event from request context here as it may have been modified
                // (necessary to avoid scribbling between thrreads)
                returnMessage = new DefaultMuleMessage(NullPayload.getInstance(), RequestContext.getEvent().getMessage());
            }
            ExceptionPayload exceptionPayload = returnMessage.getExceptionPayload();
            if (exceptionPayload == null)
            {
                exceptionPayload = new DefaultExceptionPayload(e);
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
        service.getExceptionListener().exceptionThrown(exception);
    }

    public String toString()
    {
        return "proxy for: " + service.toString();
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

    protected ReplyToHandler getReplyToHandler(MuleMessage message, InboundEndpoint endpoint)
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

    private void processReplyTo(MuleMessage returnMessage) throws MuleException
    {
        if (returnMessage != null && returnMessage.getReplyTo() != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("sending reply to: " + returnMessage.getReplyTo());
            }

            // get the endpointUri for this uri
            OutboundEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getOutboundEndpoint(returnMessage.getReplyTo().toString());
            // make sure remove the replyTo property as not cause a a forever
            // replyto loop
            returnMessage.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);

            // Create the replyTo event asynchronous
            MuleEvent replyToEvent = new DefaultMuleEvent(returnMessage, endpoint, event.getSession(), false);

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
            logger.trace("MuleProxy: async onEvent for Mule UMO " + service.getName());
        }

        try
        {
            if (event.getEndpoint() instanceof InboundEndpoint)
            {
                InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();
                // dispatch the next receiver
                event = OptimizedRequestContext.criticalSetEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), endpoint);

                // do stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }
                
                if (service.getName().startsWith("_xfireServiceComponent") ||
                    service.getName().startsWith("_axisServiceComponent"))
                {
                    // TODO MULE-2099 This is what the MethodFixInterceptor from Axis/XFire was doing.
                    event.getMessage().setBooleanProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY, true);
                }
                MuleMessage result = umo.intercept(null);
                
                if (service.getName().startsWith("_xfireServiceComponent") ||
                    service.getName().startsWith("_axisServiceComponent"))
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
                    if (service.getOutboundRouter().hasEndpoints())
                    {
                        service.getOutboundRouter().route(result, event.getSession(), event.isSynchronous());
                    }
                }

                // process replyTo if there is one
                if (result != null && replyToHandler != null)
                {
                    String requestor = (String) result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
                    {
                        replyToHandler.processReplyTo(event, result, replyTo);
                    }
                }
            }
            else
            {
                OutboundEndpoint endpoint = (OutboundEndpoint) event.getEndpoint();
                endpoint.dispatch(event);
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
                        CoreMessages.eventProcessingFailedFor(service.getName()), 
                        event.getMessage(), e));
            }
        }
    }

    public void release()
    {
        // nothing to do
    }
}
