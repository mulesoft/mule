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

import org.mule.OptimizedRequestContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.DisposeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.ComponentStatistics;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent implements Component
{

    /**
     * logger used by this class
     */
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
        // Ensure we have event in ThreadLocal
        OptimizedRequestContext.unsafeSetEvent(event);

        if (logger.isTraceEnabled())
        {
            logger.trace("Invoking " + this.getClass().getName() + "component for service " + service.getName());
        }

        // Do some checks: i) check component is not disposed, ii) that it is started
        // and iii) that the event's endpoint is an inbound endpoint.
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

        // Invoke component implementation and gather statistics
        try
        {
            long startTime = 0;
            if (statistics.isEnabled())
            {
                startTime = System.currentTimeMillis();
            }

            MuleMessage result = doOnCall(event);

            if (statistics.isEnabled())
            {
                statistics.addExecutionTime(System.currentTimeMillis() - startTime);
            }
            return result;
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new ServiceException(CoreMessages.failedToInvoke(this.toString()), event.getMessage(), service, e);
        }
    }

    protected abstract MuleMessage doOnCall(MuleEvent event) throws Exception;

    public String toString()
    {
        return this.getClass().getName() + " component for: " + service.toString();
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

    public final void initialise() throws InitialisationException
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
    }

    protected void doInitialise() throws InitialisationException
    {
        // Default implementation is no-op
    }

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

    protected void doDispose()
    {
        // Default implementation is no-op
    }

    public void stop() throws MuleException
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
    }

    protected void doStart() throws MuleException
    {
        // Default implementation is no-op
    }

    public void start() throws MuleException
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
    }

    protected void doStop() throws MuleException
    {
        // Default implementation is no-op
    }

    protected void checkDisposed() throws DisposeException
    {
        if (disposed.get())
        {
            throw new DisposeException(CoreMessages.createStaticMessage("Cannot use a disposed component"), this);
        }
    }

}
