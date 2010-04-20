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
    //protected LifecycleState lifecycleState;
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

        if (!(event.getEndpoint() instanceof InboundEndpoint))
        {
            throw new IllegalStateException(
                "Unable to process outbound event, components only process incoming events.");
        }
        if (service.getLifecycleManager().getState().isStopping() || !service.getLifecycleManager().getState().isStarted())
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
        //lifecycleState = service.getLifecycleManager().getState();
    }

    public Service getService()
    {
        return service;
    }

    public final void initialise() throws InitialisationException
    {
        //TODO with spring controlling lifecycle the component gets initialised before the service, even though there is an
        //explicit dependency
        if(service!=null && service.getLifecycleManager()!=null && service.getLifecycleManager().getState().isInitialised())
        {
            return;
        }

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
    }

    protected void doInitialise() throws InitialisationException
    {
        // Default implementation is no-op
    }

    public void dispose()
    {
        if(service.getLifecycleManager().getState().isDisposed())
        {
            return;
        }
            try
            {
                //TODO is this needed, doesn't the service manage this?
                if (service.getLifecycleManager().getState().isStarted())
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
    }

    protected void doDispose()
    {
        // Default implementation is no-op
    }

    public void stop() throws MuleException
    {
        if(service.getLifecycleManager().getState().isStopped())
        {
            return;
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping: " + this);
        }
        doStop();
    }

    protected void doStart() throws MuleException
    {
        // Default implementation is no-op
    }

    public void start() throws MuleException
    {
        if(service.getLifecycleManager().getState().isStarted())
        {
            return;
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Starting: " + this);
        }
        notificationHandler = new OptimisedNotificationHandler(service.getMuleContext()
            .getNotificationManager(), ComponentMessageNotification.class);
        doStart();
    }

    protected void doStop() throws MuleException
    {
        // Default implementation is no-op
    }

    protected void fireComponentNotification(MuleMessage message, int action)
    {
        if (notificationHandler.isNotificationEnabled(ComponentMessageNotification.class))
        {
            notificationHandler.fireNotification(new ComponentMessageNotification(message, this, action));
        }
    }

}
