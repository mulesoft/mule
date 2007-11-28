/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.seda.optimised;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.model.MuleProxy;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.model.ModelException;
import org.mule.util.queue.QueueSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleProxy</code> is a proxy to a UMO. It is a poolable object that can be
 * executed in its own thread.
 */

public class OptimisedMuleProxy implements MuleProxy
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(OptimisedMuleProxy.class);

    /**
     * Holds the current event being processed
     */
    private UMOEvent event;

    /**
     * holds the UMO descriptor
     */
    private UMOComponent component;

    /**
     * Determines if the proxy is suspended
     */
    private boolean suspended = true;

    private ComponentStatistics stat = null;

    private Callable pojoService;

    private boolean started = false;
    private boolean disposed = false;

    /**
     * Constructs a Proxy using the UMO's AbstractMessageDispatcher and the UMO
     * itself
     * 
     * @param component the underlying object that with receive events
     * @param component the UMOComponent descriptor associated with the component
     */
    public OptimisedMuleProxy(Callable pojoService, UMOComponent component)
        throws UMOException
    {
        this.component = component;
        this.pojoService = pojoService;
    }

    public void start() throws UMOException
    {
        checkDisposed();
        if (!started && pojoService instanceof Startable)
        {
            try
            {
                ((Startable) pojoService).start();
                started = true;
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
        return started;
    }

    public void stop() throws UMOException
    {
        checkDisposed();

        if (started && pojoService instanceof Stoppable)
        {
            started = false;
            try
            {
                ((Stoppable) pojoService).stop();
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
    public void onEvent(QueueSession session, UMOEvent event)
    {
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
     * @throws org.mule.umo.UMOException if the call fails
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
            if (event.getEndpoint().canRequest())
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
                    logger.debug("Event stop further processing has been set, no outbound routing will be performed.");
                }
                if (returnMessage != null && !event.isStopFurtherProcessing())
                {
                    // Map context = RequestContext.clearProperties();
                    // if (context != null) {
                    // returnMessage.addProperties(context);
                    // }
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
                // if (returnMessage != null && component.getResponseRouter() !=
                // null) {
                // logger.debug("Waiting for response router message");
                // returnMessage =
                // component.getResponseRouter().getResponse(returnMessage);
                // }
                //
                // // process repltyTo if there is one
                // if (returnMessage != null && replyToHandler != null) {
                // String requestor = (String)
                // returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                // if ((requestor != null && !requestor.equals(component.getName()))
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
            if (e instanceof UMOException)
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
        return returnMessage;
    }

    protected UMOMessage invokeUmo(UMOEventContext context) throws Exception
    {
        Object result = pojoService.onCall(RequestContext.getEventContext());
        if (result != null)
        {
            if (result instanceof UMOMessage)
            {
                return (UMOMessage) result;
            }
            else
            {
                return new MuleMessage(result, context.getMessage());
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
        component.getExceptionListener().exceptionThrown(exception);
    }

    public String toString()
    {
        return "optimised proxy for: " + component.toString();
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

    // private void processReplyTo(UMOMessage returnMessage) throws UMOException
    // {
    // if (returnMessage != null && returnMessage.getReplyTo() != null) {
    // logger.info("sending reply to: " + returnMessage.getReplyTo());
    // UMOEndpointURI endpointUri = new
    // MuleEndpointURI(returnMessage.getReplyTo().toString());
    //
    // // get the endpointUri for this uri
    // UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri,
    // UMOEndpoint.ENDPOINT_TYPE_SENDER);
    //
    // // Create the replyTo event asynchronous
    // UMOEvent replyToEvent = new MuleEvent(returnMessage, endpoint,
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
            logger.trace("MuleProxy: async onEvent for Mule UMO " + component.getName());
        }

        try
        {
            if (event.getEndpoint().canRequest())
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
                // InterceptorsInvoker(interceptorList, component,
                // event.getMessage());

                // do stats
                long startTime = 0;
                if (stat.isEnabled())
                {
                    startTime = System.currentTimeMillis();
                }
                UMOMessage result = invokeUmo(RequestContext.getEventContext());
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

                // process repltyTo if there is one
                // if (result != null && replyToHandler != null) {
                // String requestor = (String)
                // result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                // if ((requestor != null && !requestor.equals(component.getName()))
                // || requestor == null) {
                // replyToHandler.processReplyTo(event, result, replyTo);
                // }
                // }
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
            if (e instanceof UMOException)
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
        finally
        {

            try
            {
                //component.getServiceFactory().release(umo);
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
