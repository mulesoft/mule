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

import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.VoidResult;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.interceptor.Invocation;
import org.mule.api.lifecycle.DisposeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.ComponentMessageNotification;
import org.mule.context.notification.OptimisedNotificationHandler;
import org.mule.management.stats.ComponentStatistics;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent implements Component, Interceptor, MuleContextAware
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
    protected ServerNotificationHandler notificationHandler;
    protected List interceptors = new ArrayList();
    protected MuleContext muleContext;


    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public List getInterceptors()
    {
        return interceptors;
    }

    public void setInterceptors(List interceptors)
    {
        this.interceptors = interceptors;
    }

    public AbstractComponent()
    {
        statistics = new ComponentStatistics();
    }

    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        return invokeInternal(invocation.getEvent());
    }

    private MuleMessage invokeInternal(MuleEvent event)
        throws DisposeException, DefaultMuleException, MuleException, ServiceException
    {
        // Ensure we have event in ThreadLocal
        OptimizedRequestContext.unsafeSetEvent(event);

        if (logger.isTraceEnabled())
        {
            logger.trace("Invoking " + this.getClass().getName() + "component for service "
                         + service.getName());
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

            fireComponentNotification(event.getMessage(), ComponentMessageNotification.COMPONENT_PRE_INVOKE);

            long startTime = 0;
            if (statistics.isEnabled())
            {
                startTime = System.currentTimeMillis();
            }

            Object result = doInvoke(event);

            if (statistics.isEnabled())
            {
                statistics.addExecutionTime(System.currentTimeMillis() - startTime);
            }

            MuleMessage resultMessage = createResultMessage(event, result);

            fireComponentNotification(resultMessage, ComponentMessageNotification.COMPONENT_POST_INVOKE);

            return resultMessage;
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new ServiceException(CoreMessages.failedToInvoke(this.toString()), event.getMessage(),
                service, e);
        }
    }

    public MuleMessage invoke(MuleEvent event) throws MuleException
    {
        if (interceptors.isEmpty())
        {
            return invokeInternal(event);
        }
        else
        {
            return new ComponentInterceptorInvoker(this, interceptors, event).invoke();
        }
    }

    protected MuleMessage createResultMessage(MuleEvent event, Object result) throws TransformerException
    {
        if (result instanceof MuleMessage)
        {
            return (MuleMessage) result;
        }
        else if (result instanceof VoidResult)
        {
            event.transformMessage();
            return event.getMessage();
        }
        else if (result != null)
        {
            event.getMessage().applyTransformers(
                    Collections.<Transformer>singletonList(new TransformerTemplate(
                        new TransformerTemplate.OverwitePayloadCallback(result))));
            return event.getMessage();
        }
        else
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }
    }

    protected abstract Object doInvoke(MuleEvent event) throws Exception;

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(this.getClass().getName());
        
        if (service != null)
        {
            buf.append(" component for: ").append(service.toString());
        }
        else
        {
            buf.append(" no component");
        }
        
        return buf.toString();
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

        // propagate MuleContext from the enclosing service if provided
        if (this.muleContext == null && service.getMuleContext() != null)
        {
            this.muleContext = service.getMuleContext();
        }
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
        if (!disposed.get())
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
            notificationHandler = new OptimisedNotificationHandler(service.getMuleContext()
                .getNotificationManager(), ComponentMessageNotification.class);
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
            throw new DisposeException(CoreMessages.createStaticMessage("Cannot use a disposed component"),
                this);
        }
    }

    protected void fireComponentNotification(MuleMessage message, int action)
    {
        if (notificationHandler.isNotificationEnabled(ComponentMessageNotification.class))
        {
            notificationHandler.fireNotification(new ComponentMessageNotification(message, this, action));
        }
    }

}
