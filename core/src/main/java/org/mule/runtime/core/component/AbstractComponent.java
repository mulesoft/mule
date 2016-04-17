/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.context.notification.ComponentMessageNotification;
import org.mule.runtime.core.context.notification.OptimisedNotificationHandler;
import org.mule.runtime.core.management.stats.ComponentStatistics;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.transformer.TransformerTemplate;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent extends AbstractAnnotatedObject implements Component, MuleContextAware, Lifecycle, MessagingExceptionHandlerAware
{

    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(this.getClass());

    protected FlowConstruct flowConstruct;
    protected ComponentStatistics statistics = null;
    protected ServerNotificationHandler notificationHandler;
    protected List<Interceptor> interceptors = new ArrayList<Interceptor>();
    protected MessageProcessorChain interceptorChain;
    protected MuleContext muleContext;
    protected ComponentLifecycleManager lifecycleManager;
    private MessagingExceptionHandler messagingExceptionHandler;

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
        lifecycleManager = new ComponentLifecycleManager(getName(), this);
    }

    private MuleEvent invokeInternal(MuleEvent event) throws MuleException
    {
        // Ensure we have event in ThreadLocal
        OptimizedRequestContext.unsafeSetEvent(event);

        if (logger.isTraceEnabled())
        {
            logger.trace(String.format("Invoking %s component for service %s", this.getClass().getName(),
                flowConstruct.getName()));
        }

        if (!lifecycleManager.getState().isStarted() || lifecycleManager.getState().isStopping())
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
            // Components only have access to the original event, so propogate the
            // stop further processing
            resultEvent.setStopFurtherProcessing(event.isStopFurtherProcessing());
            fireComponentNotification(resultEvent.getMessage(),
                ComponentMessageNotification.COMPONENT_POST_INVOKE);

            return resultEvent;
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new ComponentException(CoreMessages.failedToInvoke(this.toString()), event, this, e);
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
            event.setMessage(
                    muleContext.getTransformationService().applyTransformers(event.getMessage(), event, Collections
                            .<Transformer>singletonList
                            (new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(result)))));
            return event;
        }
        else
        {
            DefaultMuleMessage emptyMessage = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            emptyMessage.propagateRootId(event.getMessage());
            return new DefaultMuleEvent(emptyMessage, event);
        }
    }

    protected abstract Object doInvoke(MuleEvent event) throws Exception;

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
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
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    public final void initialise() throws InitialisationException
    {
        if (flowConstruct == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Component has not been initialized properly, no flow constuct."),
                this);
        }

        lifecycleManager.fireInitialisePhase(new LifecycleCallback<Component>()
        {
            public void onTransition(String phaseName, Component object) throws MuleException
            {
                DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder(
                    flowConstruct);
                chainBuilder.setName("Component interceptor processor chain for :" + getName());
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
                interceptorChain = chainBuilder.build();
                applyLifecycleAndDependencyInjection(interceptorChain);
                doInitialise();
            }
        });
    }

    protected void applyLifecycleAndDependencyInjection(Object object) throws InitialisationException
    {
        if (object instanceof MuleContextAware)
        {
            ((MuleContextAware) object).setMuleContext(muleContext);
        }
        if (object instanceof MessagingExceptionHandlerAware)
        {
            ((MessagingExceptionHandlerAware) object).setMessagingExceptionHandler(messagingExceptionHandler);
        }
        if (object instanceof Initialisable)
        {
            ((Initialisable) object).initialise();
        }
    }

    protected void doInitialise() throws InitialisationException
    {
        // Default implementation is no-op
    }

    public void dispose()
    {
        lifecycleManager.fireDisposePhase(new LifecycleCallback<Component>()
        {
            public void onTransition(String phaseName, Component object) throws MuleException
            {
                doDispose();
            }
        });
    }

    protected void doDispose()
    {
        // Default implementation is no-op
    }

    public void stop() throws MuleException
    {
        try
        {
            lifecycleManager.fireStopPhase(new LifecycleCallback<Component>()
            {
                public void onTransition(String phaseName, Component object) throws MuleException
                {
                    doStop();
                }
            });
        }
        catch (MuleException e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    protected void doStart() throws MuleException
    {
        // Default implementation is no-op
    }

    public void start() throws MuleException
    {
        lifecycleManager.fireStartPhase(new LifecycleCallback<Component>()
        {
            public void onTransition(String phaseName, Component object) throws MuleException
            {
                notificationHandler = new OptimisedNotificationHandler(muleContext.getNotificationManager(),
                    ComponentMessageNotification.class);
                doStart();
            }
        });

    }

    protected void doStop() throws MuleException
    {
        // Default implementation is no-op
    }

    protected void fireComponentNotification(MuleMessage message, int action)
    {
        if (notificationHandler != null
            && notificationHandler.isNotificationEnabled(ComponentMessageNotification.class))
        {
            notificationHandler.fireNotification(new ComponentMessageNotification(message, this,
                flowConstruct, action));
        }
    }

    protected String getName()
    {
        StringBuilder sb = new StringBuilder();
        if (flowConstruct != null)
        {
            sb.append(flowConstruct.getName());
            sb.append(".");
        }
        sb.append("component");
        sb.append(".");
        sb.append(System.identityHashCode(this));
        return sb.toString();
    }

    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.messagingExceptionHandler = messagingExceptionHandler;
    }
}
