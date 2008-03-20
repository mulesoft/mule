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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.DisposeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.ComponentStatistics;
import org.mule.transport.AbstractConnector;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent implements Component
{

    /** logger used by this class */
    protected final Log logger = LogFactory.getLog(this.getClass());

    protected Service service;
    protected ComponentStatistics statistics = null;
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean stopping = new AtomicBoolean(false);
    protected final AtomicBoolean initialised = new AtomicBoolean(false);
    protected final AtomicBoolean disposing = new AtomicBoolean(false);
    protected final AtomicBoolean disposed = new AtomicBoolean(false);

    public AbstractComponent()
    {
        statistics = new ComponentStatistics();
    }

    public MuleMessage onCall(MuleEvent event) throws MuleException
    {
        if (logger.isTraceEnabled())
        {
            logger.trace(this.getClass().getName() + ": sync call for Mule Component " + service.getName());
        }
        checkDisposed();
        if (!(event.getEndpoint() instanceof InboundEndpoint))
        {
            throw new IllegalStateException(
                "Unable to process outbound event, components only process incoming events.");
        }
        if (stopping.get() || !started.get())
        {
            throw new DefaultMuleException(CoreMessages.componentIsStopped(service.getName()));
        }
        try
        {
            return doOnCall(event);
        }
        catch (Exception e)
        {
            throw new ServiceException(CoreMessages.failedToInvoke(this.toString()), event.getMessage(), service, e);
        }
    }

    public void onEvent(MuleEvent event)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace(this.getClass().getName() + ": async call for Mule Component " + service.getName());
        }
        try
        {
            checkDisposed();
            if (!(event.getEndpoint() instanceof InboundEndpoint))
            {
                throw new IllegalStateException(
                    "Unable to process outbound event, components only process incoming events.");
            }
            if (stopping.get() || !started.get())
            {
                throw new DefaultMuleException(CoreMessages.componentIsStopped(service.getName()));
            }
            doOnEvent(event);
        }
        catch (Exception e)
        {
            logger.error(new ServiceException(CoreMessages.failedToInvoke(this.toString()), event.getMessage(),
                service, e));
        }
    }

    protected abstract MuleMessage doOnCall(MuleEvent event);

    protected abstract void doOnEvent(MuleEvent event);

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

    public void release()
    {
        // nothing to do
    }

    public ComponentStatistics getStatistics()
    {
        return statistics;
    }

    public void setService(Service service)
    {
        this.service = service;
    }

    public Service getService()
    {
        return service;
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        if (!initialised.get())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Initialising: " + this);
            }
            if (service == null)
            {
                throw new InitialisationException(
                    MessageFactory.createStaticMessage("Component has not been initialized properly, no service."),
                    this);
            }
            doInitialise();
            initialised.set(true);
        }
        return LifecycleTransitionResult.OK;
    }

    protected abstract void doInitialise() throws InitialisationException;

    public void dispose()
    {
        disposing.set(true);
        try
        {
            if (started.get())
            {
                stop();
            }
        }
        catch (MuleException e)
        {
            logger.error(CoreMessages.failedToStop(toString()));
        }
        try
        {
            doDispose();
        }
        catch (Exception e)
        {
            logger.warn(CoreMessages.failedToDispose(toString()), e);
        }
        finally
        {
            disposed.set(true);
            disposing.set(false);
            initialised.set(false);
        }
    }

    protected abstract void doDispose();

    public LifecycleTransitionResult stop() throws MuleException
    {
        // If component is already disposed then ignore, don't fails, as stop() might
        // get called by service after spring has called disposed etc.
        if (!disposed.get() && started.get() && !stopping.get())
        {
            stopping.set(true);
            if (logger.isInfoEnabled())
            {
                logger.info("Stopping: " + this);
            }
            doStop();
            started.set(false);
            stopping.set(false);
        }
        return LifecycleTransitionResult.OK;
    }

    protected abstract void doStart() throws MuleException;

    public LifecycleTransitionResult start() throws MuleException
    {
        checkDisposed();
        if (!started.get())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Starting: " + this);
            }
            doStart();
            started.set(true);
        }
        return LifecycleTransitionResult.OK;
    }

    protected abstract void doStop() throws MuleException;

    protected void checkDisposed() throws DisposeException
    {
        if (disposed.get())
        {
            throw new DisposeException(CoreMessages.createStaticMessage("Cannot use a disposed component"), this);
        }
    }

}
