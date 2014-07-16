/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.VoidResult;
import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.SimpleService;
import org.mule.context.notification.ComponentMessageNotification;
import org.mule.context.notification.OptimisedNotificationHandler;
import org.mule.management.stats.ComponentStatistics;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link Component} to be used by all {@link Component} implementations.
 */
public abstract class AbstractComponent implements Component, MuleContextAware, Lifecycle, AnnotatedObject, MessagingExceptionHandlerAware
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
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

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
            event.getMessage().applyTransformers(
                event,
                Collections.<Transformer> singletonList(new TransformerTemplate(
                    new TransformerTemplate.OverwitePayloadCallback(result))));
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
        if (!(flowConstruct instanceof Service || flowConstruct instanceof SimpleService))
        {
            sb.append(".");
            sb.append(System.identityHashCode(this));
        }
        return sb.toString();
    }

    public final Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }

    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.messagingExceptionHandler = messagingExceptionHandler;
    }
}
