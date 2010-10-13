/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.VoidResult;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.ComponentMessageNotification;
import org.mule.context.notification.OptimisedNotificationHandler;
import org.mule.management.stats.ComponentStatistics;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
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
public abstract class AbstractComponent implements Component, MuleContextAware, Lifecycle
{

    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(this.getClass());

    protected FlowConstruct flowConstruct;
    protected ComponentStatistics statistics = null;
    protected ServerNotificationHandler notificationHandler;
    protected List<Interceptor> interceptors = new ArrayList<Interceptor>();
    protected MessageProcessor interceptorChain;
    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public List<Interceptor> getInterceptors()
    {
        return interceptors;
    }

    public void setInterceptors(List<Interceptor> interceptors)
    {
        this.interceptors = interceptors;
    }

    public AbstractComponent()
    {
        statistics = new ComponentStatistics();
    }

    private MuleEvent invokeInternal(MuleEvent event) throws MuleException
    {
        // Ensure we have event in ThreadLocal
        OptimizedRequestContext.unsafeSetEvent(event);

        if (logger.isTraceEnabled())
        {
            logger.trace("Invoking " + this.getClass().getName() + "component for service "
                         + flowConstruct.getName());
        }

        if (!flowConstruct.getLifecycleState().isStarted() || flowConstruct.getLifecycleState().isStopping())
        {
            throw new LifecycleException(CoreMessages.isStopped(flowConstruct.getName()), this);
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

            MuleEvent resultEvent = createResultEvent(event, result);
            // Components only have access to the original event, so propogate the stop further processing 
            resultEvent.setStopFurtherProcessing(event.isStopFurtherProcessing());
            fireComponentNotification(resultEvent.getMessage(), ComponentMessageNotification.COMPONENT_POST_INVOKE);

            return resultEvent;
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new ComponentException(CoreMessages.failedToInvoke(this.toString()), event,
                this, e);
        }
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (interceptorChain == null)
        {
            return invokeInternal(event);
        }
        else
        {
            return interceptorChain.process(event);
        }
    }

    protected MuleEvent createResultEvent(MuleEvent event, Object result) throws MuleException
    {
        if (result instanceof MuleMessage)
        {
            return new DefaultMuleEvent((MuleMessage) result, event);
        }
        else if (result instanceof VoidResult)
        {
            return event;
        }
        else if (result != null)
        {
            event.getMessage().applyTransformers(
                    event, Collections.<Transformer>singletonList(new TransformerTemplate(
                        new TransformerTemplate.OverwitePayloadCallback(result))));
            return event;
        }
        else
        {
            return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), event);
        }
    }

    protected abstract Object doInvoke(MuleEvent event) throws Exception;

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(this.getClass().getName());
        
        if (flowConstruct != null)
        {
            buf.append(" component for: ").append(flowConstruct.toString());
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

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;

        // propagate MuleContext from the enclosing service if provided
        if (this.muleContext == null && flowConstruct.getMuleContext() != null)
        {
            this.muleContext = flowConstruct.getMuleContext();
        }
        //lifecycleState = service.getLifecycleManager().getState();
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    public final void initialise() throws InitialisationException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Initialising: " + this);
        }
        if (flowConstruct == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Component has not been initialized properly, no service."),
                this);
        }
        
        DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder(flowConstruct);
        for (Interceptor interceptor : interceptors)
        {
            chainBuilder.chain(interceptor);
        }
        chainBuilder.chain(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return invokeInternal(event);
            }
        });
        
        try
        {
            interceptorChain = chainBuilder.build();
            if (interceptorChain instanceof Initialisable)
            {
                ((Initialisable) interceptorChain).initialise();
            }
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        
        doInitialise();
    }

    protected void doInitialise() throws InitialisationException
    {
        // Default implementation is no-op
    }

    public void dispose()
    {
        if(flowConstruct.getLifecycleState().isDisposed())
        {
            return;
        }
    
        try
        {
            // TODO is this needed, doesn't the service manage this?
            if (flowConstruct.getLifecycleState().isStarted())
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
        if(flowConstruct.getLifecycleState().isStopped())
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
        if(flowConstruct.getLifecycleState().isStarted())
        {
            return;
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Starting: " + this);
        }
        notificationHandler = new OptimisedNotificationHandler(flowConstruct.getMuleContext()
            .getNotificationManager(), ComponentMessageNotification.class);
        doStart();
    }

    protected void doStop() throws MuleException
    {
        // Default implementation is no-op
    }

    protected void fireComponentNotification(MuleMessage message, int action)
    {
        if (notificationHandler != null && notificationHandler.isNotificationEnabled(ComponentMessageNotification.class))
        {
            notificationHandler.fireNotification(new ComponentMessageNotification(message, this, flowConstruct, action));
        }
    }

}
