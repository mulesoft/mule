/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.seda.optimised;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.ModelException;
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.ServiceStatistics;
import org.mule.util.queue.QueueSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleProxy</code> is a proxy to a UMO. It is a poolable object that can be
 * executed in its own thread.
 */

public class OptimisedJavaComponent implements Component
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(OptimisedJavaComponent.class);

    /**
     * Holds the current event being processed
     */
    private MuleEvent event;

    /**
     * holds the UMO descriptor
     */
    private Service service;

    /**
     * Determines if the proxy is suspended
     */
    private boolean suspended = true;

    private ServiceStatistics stat = null;

    private Callable pojoService;

    private boolean started = false;
    private boolean disposed = false;

    /**
     * Constructs a Proxy using the UMO's AbstractMessageDispatcher and the UMO
     * itself
     * 
     * @param service the underlying object that with receive events
     * @param service the Service descriptor associated with the service
     */
    public OptimisedJavaComponent(Callable pojoService, Service service)
        throws MuleException
    {
        this.service = service;
        this.pojoService = pojoService;
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        checkDisposed();
        if (!started && pojoService instanceof Startable)
        {
            try
            {
                return LifecycleTransitionResult.startOrStopAll(((Startable) pojoService).start(), new LifecycleTransitionResult.Closure()
                {
                    public LifecycleTransitionResult doContinue()
                    {
                        started = true;
                        return LifecycleTransitionResult.OK;
                    }});
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStart("Service '" + service.getName() + "'"), e);
            }
        }
        else
        {
            return LifecycleTransitionResult.OK;
        }
    }

    public boolean isStarted()
    {
        return started;
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        checkDisposed();

        if (started && pojoService instanceof Stoppable)
        {
            started = false;
            try
            {
                return ((Stoppable) pojoService).stop();
            }
            catch (Exception e)
            {
                throw new ModelException(
                    CoreMessages.failedToStop("Service '" + service.getName() + "'"), e);
            }
        }
        else
        {
            return LifecycleTransitionResult.OK;
        }
    }

    public void dispose()
    {
        checkDisposed();
        if (pojoService instanceof Disposable)
        {
            ((Disposable) pojoService).dispose();
            disposed = true;
        }
    }

    private void checkDisposed()
    {
        if (disposed)
        {
            throw new IllegalStateException("Components Disposed Of");
        }
    }

    /**
     * Sets the current event being processed
     * 
     * @param event the event being processed
     */
    public void onEvent(QueueSession session, MuleEvent event)
    {
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
     * @throws org.mule.api.MuleException if the call fails
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
                // RequestContext.setEvent(event);
                // Object replyTo = event.getMessage().getReplyTo();
                // ReplyToHandler replyToHandler = null;
                // if (replyTo != null) {
                // replyToHandler = ((AbstractConnector)
                // event.getEndpoint().getConnector()).getReplyToHandler();
                // }

                // stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }
                returnMessage = invokeUmo(RequestContext.getEventContext());
                // stats
                if (stat.isEnabled())
                {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // this is the request event
                event = RequestContext.getEvent();
                if (event.isStopFurtherProcessing())
                {
                    logger.debug("MuleEvent stop further processing has been set, no outbound routing will be performed.");
                }
                if (returnMessage != null && !event.isStopFurtherProcessing())
                {
                    // Map context = RequestContext.clearProperties();
                    // if (context != null) {
                    // returnMessage.addProperties(context);
                    // }
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
                // if (returnMessage != null && service.getResponseRouter() !=
                // null) {
                // logger.debug("Waiting for response router message");
                // returnMessage =
                // service.getResponseRouter().getResponse(returnMessage);
                // }
                //
                // // process repltyTo if there is one
                // if (returnMessage != null && replyToHandler != null) {
                // String requestor = (String)
                // returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                // if ((requestor != null && !requestor.equals(service.getName()))
                // || requestor == null) {
                // replyToHandler.processReplyTo(event, returnMessage, replyTo);
                // }
                // }

            }
            else
            {
                returnMessage = event.getSession().sendEvent(event);
                // processReplyTo(returnMessage);
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
            if (e instanceof MuleException)
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
        return returnMessage;
    }

    protected MuleMessage invokeUmo(MuleEventContext context) throws Exception
    {
        Object result = pojoService.onCall(RequestContext.getEventContext());
        if (result != null)
        {
            if (result instanceof MuleMessage)
            {
                return (MuleMessage) result;
            }
            else
            {
                return new DefaultMuleMessage(result, context.getMessage());
            }
        }
        return null;
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
        return "optimised proxy for: " + service.toString();
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

    /**
     * Controls the suspension of the UMO event processing
     */
    public void suspend()
    {
        suspended = true;
    }

    /**
     * Triggers the UMO to resume processing of events if it is suspended
     */
    public void resume()
    {
        suspended = false;
    }

    // private void processReplyTo(MuleMessage returnMessage) throws MuleException
    // {
    // if (returnMessage != null && returnMessage.getReplyTo() != null) {
    // logger.info("sending reply to: " + returnMessage.getReplyTo());
    // EndpointURI endpointUri = new
    // MuleEndpointURI(returnMessage.getReplyTo().toString());
    //
    // // get the endpointUri for this uri
    // Endpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri,
    // Endpoint.ENDPOINT_TYPE_SENDER);
    //
    // // Create the replyTo event asynchronous
    // MuleEvent replyToEvent = new DefaultMuleEvent(returnMessage, endpoint,
    // event.getSession(), false);
    // // make sure remove the replyTo property as not cause a a forever
    // // replyto loop
    // replyToEvent.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    //
    // // queue the event
    // onEvent(queueSession, replyToEvent);
    // logger.info("reply to sent: " + returnMessage.getReplyTo());
    // if (stat.isEnabled()) {
    // stat.incSentReplyToEvent();
    // }
    // }
    // }

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
                // dispatch the next receiver
                event = RequestContext.setEvent(event);
                // Object replyTo = event.getMessage().getReplyTo();
                // ReplyToHandler replyToHandler = null;
                // if (replyTo != null) {
                // replyToHandler = ((AbstractConnector)
                // event.getEndpoint().getConnector()).getReplyToHandler();
                // }
                // InterceptorsInvoker invoker = new
                // InterceptorsInvoker(interceptorList, service,
                // event.getMessage());

                // do stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }
                MuleMessage result = invokeUmo(RequestContext.getEventContext());
                if (stat.isEnabled())
                {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // processResponse(result, replyTo, replyToHandler);
                event = RequestContext.getEvent();
                if (result != null && !event.isStopFurtherProcessing())
                {
                    service.getOutboundRouter().route(result, event.getSession(), event.isSynchronous());
                }

                // process repltyTo if there is one
                // if (result != null && replyToHandler != null) {
                // String requestor = (String)
                // result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                // if ((requestor != null && !requestor.equals(service.getName()))
                // || requestor == null) {
                // replyToHandler.processReplyTo(event, result, replyTo);
                // }
                // }
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
            if (e instanceof MuleException)
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
        finally
        {

            try
            {
                //service.getServiceFactory().release(umo);
            }
            catch (Exception e2)
            {
                // TODO MULE-863: If this is an error, do something about it
                logger.error("Failed to return proxy: " + e2.getMessage(), e2);
            }
        }
    }

    public void release()
    {
        // nothing to do
    }

    public Callable getPojoService()
    {
        return pojoService;
    }
}
