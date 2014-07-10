/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.AnnotatedObject;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.RouterStatisticsRecorder;
import org.mule.api.service.Service;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.ErrorHandlingExecutionTemplate;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.lifecycle.processor.ProcessIfStartedWaitIfPausedMessageProcessor;
import org.mule.management.stats.RouterStatistics;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.service.processor.ServiceAsyncRequestReplyRequestor;
import org.mule.source.ClusterizableMessageSourceWrapper;
import org.mule.util.ClassUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base implementation for all Services in Mule
 */
@Deprecated
public abstract class AbstractService implements Service, MessageProcessor, AnnotatedObject
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ServiceStatistics stats;
    /**
     * The model in which this service is registered
     */
    protected Model model;

    protected MuleContext muleContext;

    protected ServiceLifecycleManager lifecycleManager;

    /**
     * The initial states that the service can be started in
     */
    public static final String INITIAL_STATE_STOPPED = "stopped";
    public static final String INITIAL_STATE_STARTED = "started";
    public static final String INITIAL_STATE_PAUSED = "paused";

    /**
     * The exception strategy used by the service.
     */
    protected MessagingExceptionHandler exceptionListener;

    /**
     * The service's name
     */
    protected String name;

    protected MessageProcessor outboundRouter = new DefaultOutboundRouterCollection();

    protected MessageSource messageSource = new ServiceCompositeMessageSource();
    protected ServiceAsyncReplyCompositeMessageSource asyncReplyMessageSource = new ServiceAsyncReplyCompositeMessageSource();

    protected MessageProcessorChain messageProcessorChain;
    protected MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    /**
     * Determines the initial state of this service when the model starts. Can be
     * 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    // Default component to use if one is not configured.
    // TODO MULE-3113 This should not really be needed as to implement bridging we
    // should
    // should just increment a 'bridged' counter and sent the event straight to
    // outbound router collection. Currently it's the Component that routes events
    // onto the outbound router collection so this default implementation is needed.
    // It would be beneficial to differenciate between component invocations and
    // events that are bridged but currently everything is an invocation.
    protected Component component = new PassThroughComponent();

    public AbstractService(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        ((MuleContextAware) component).setMuleContext(muleContext);
        try
        {
            lifecycleManager = new ServiceLifecycleManager(this, muleContext);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate("Service Lifecycle Manager"), e);
        }

    }

    //----------------------------------------------------------------------------------------//
    //-                    LIFECYCLE METHODS
    //----------------------------------------------------------------------------------------//

    /**
     * Initialise the service. The service will first create a component from the
     * ServiceDescriptor and then initialise a pool based on the attributes in the
     * ServiceDescriptor .
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *          if the service fails to
     *          initialise
     * @see org.mule.api.registry.ServiceDescriptor
     */
    public final synchronized void initialise() throws InitialisationException
    {
        logger.warn(CoreMessages.servicesDeprecated());
        
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    if (exceptionListener == null)
                    {
                        // By default use the model Exception Listener
                        // TODO MULE-2102 This should be configured in the default template.
                        exceptionListener = getModel().getExceptionListener();
                    }
                    injectFlowConstructMuleContextExceptionHandler(outboundRouter);
                    injectFlowConstructMuleContextExceptionHandler(messageSource);
                    injectFlowConstructMuleContextExceptionHandler(asyncReplyMessageSource);
                    injectFlowConstructMuleContextExceptionHandler(messageProcessorChain);
                    injectFlowConstructMuleContextExceptionHandler(component);
                    injectFlowConstructMuleContext(exceptionListener);
                    
                    doInitialise();
                }
            });
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }

    }

    public void start() throws MuleException
    {
        if (!isStopped() && initialState.equals(AbstractService.INITIAL_STATE_STOPPED))
        {
            //Transition to a stopped state without changing state of the flow construct
            lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<FlowConstruct>());
            lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<FlowConstruct>());

            logger.info("Service " + name + " has not been started (initial state = 'stopped')");
            return;
        }
        
        lifecycleManager.fireStartPhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                doStart();
            }
        });

        //Cannot call one lifecycle phase from within another, so we pause if necessary here
        if ( initialState.equals(AbstractService.INITIAL_STATE_PAUSED))
        {
            pause();
            logger.info("Service " + name + " has been started and paused (initial state = 'paused')");
        }

    }

    /**
     * Pauses event processing for a single Mule Service. Unlike stop(), a paused
     * service will still consume messages from the underlying transport, but those
     * messages will be queued until the service is resumed.
     */
    public final void pause() throws MuleException
    {
        lifecycleManager.firePausePhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                doPause();
            }
        });
    }

    /**
     * Resumes a single Mule Service that has been paused. If the service is not
     * paused nothing is executed.
     */
    public final void resume() throws MuleException
    {
        lifecycleManager.fireResumePhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                doResume();
            }
        });
    }


    public void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                doStop();
            }
        });
    }

    public final void dispose()
    {

        try
        {
            if (isStarted() || isPaused())
            {
                stop();
            }

            lifecycleManager.fireDisposePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    doDispose();
                }
            });
        }
        catch (MuleException e)
        {
            logger.error("Failed to stop service: " + name, e);
        }
    }

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    public boolean isStarted()
    {
        return lifecycleManager.getState().isStarted();
    }

    /**
     * Determines if the service is in a paused state
     * 
     * @return True if the service is in a paused state, false otherwise
     */
    public boolean isPaused()
    {
        return lifecycleManager.getCurrentPhase().equals(Pausable.PHASE_NAME);
    }

    public boolean isStopped()
    {
        return lifecycleManager.getState().isStopped();
    }

    public boolean isStopping()
    {
        return lifecycleManager.getState().isStopping();
    }

    /**
     * Custom components can execute code necessary to put the service in a paused state here. If a developer
     * overloads this method the doResume() method MUST also be overloaded to avoid inconsistent state in the
     * service
     * 
     * @throws MuleException
     */
    protected void doPause() throws MuleException
    {
        // template method
    }

    /**
     * Custom components can execute code necessary to resume a service once it has
     * been paused If a developer overloads this method the doPause() method MUST
     * also be overloaded to avoid inconsistent state in the service
     *
     * @throws MuleException
     */
    protected void doResume() throws MuleException
    {
        // template method
    }

    protected void doForceStop() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        stopIfStoppable(messageSource);
        asyncReplyMessageSource.stop();

        stopIfStoppable(messageProcessorChain);
        // Component is not in chain
        stopIfStoppable(component);
        stopIfStoppable(exceptionListener);
    }

    protected void doStart() throws MuleException
    {
        // Component is not in chain
        startIfStartable(exceptionListener);
        startIfStartable(component);
        startIfStartable(messageProcessorChain);

        startIfStartable(messageSource);
        if (asyncReplyMessageSource.getEndpoints().size() > 0)
        {
            asyncReplyMessageSource.start();
        }
    }

    protected void doDispose()
    {
        // Component is not in chain
        disposeIfDisposable(component);
        disposeIfDisposable(messageProcessorChain);
        disposeIfDisposable(messageSource);
        disposeIfDisposable(exceptionListener);
        muleContext.getStatistics().remove(stats);
    }

    protected void doInitialise() throws InitialisationException
    {
        // initialise statistics
        stats = createStatistics();
        stats.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(stats);
        RouterStatistics routerStatistics = null;

        // If the router collection already has router statistics, keep using them.
        if (outboundRouter instanceof OutboundRouterCollection)
        {
            routerStatistics = ((OutboundRouterCollection)outboundRouter).getRouterStatistics();
        }
        if (routerStatistics == null)
        {
            routerStatistics = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
        }
        stats.setOutboundRouterStat(routerStatistics);
        if (outboundRouter != null && outboundRouter instanceof RouterStatisticsRecorder)
        {
            ((RouterStatisticsRecorder)outboundRouter).setRouterStatistics(routerStatistics);
        }
        RouterStatistics inboundRouterStatistics = new RouterStatistics(RouterStatistics.TYPE_INBOUND);
        stats.setInboundRouterStat(inboundRouterStatistics);
        if (messageSource instanceof RouterStatisticsRecorder)
        {
            ((RouterStatisticsRecorder) messageSource).setRouterStatistics(inboundRouterStatistics);
        }
        stats.setComponentStat(component.getStatistics());

        try
        {
            buildServiceMessageProcessorChain();
            injectFlowConstructMuleContextExceptionHandler(messageProcessorChain);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        
        // Wrap chain to decouple lifecycle
        messageSource.setListener(new AbstractInterceptingMessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return messageProcessorChain.process(event);
            }
        });

        initialiseIfInitialisable(exceptionListener);
        initialiseIfInitialisable(component);
        initialiseIfInitialisable(messageProcessorChain);
        initialiseIfInitialisable(messageSource);
        
        if (asyncReplyMessageSource.getEndpoints().size() > 0)
        {
            asyncReplyMessageSource.initialise();
        }
    }

    public void forceStop() throws MuleException
    {
        // Kepping this here since I don't understand why this method exists. AFAICS
        // this just says the service is stopped
        // without actually stopping it
        // if (!stopped.get())
        // {
        // logger.debug("Stopping Service");
        // stopping.set(true);
        // fireServiceNotification(ServiceNotification.SERVICE_STOPPING);
        // doForceStop();
        // stopped.set(true);
        // stopping.set(false);
        // fireServiceNotification(ServiceNotification.SERVICE_STOPPED);
        // }
        doForceStop();
        stop();
    }


    //----------------------------------------------------------------------------------------//
    //-                    END LIFECYCLE METHODS
    //----------------------------------------------------------------------------------------//

    protected void buildServiceMessageProcessorChain() throws MuleException
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(this);
        builder.setName("Service '" + name + "' Processor Chain");
        builder.chain(getServiceStartedAssertingMessageProcessor());
        addMessageProcessors(builder);
        messageProcessorChain = builder.build();
    }

    protected MessageProcessor getServiceStartedAssertingMessageProcessor()
    {
        return new ProcessIfStartedWaitIfPausedMessageProcessor(this, lifecycleManager.getState());
    }

    protected abstract void addMessageProcessors(MessageProcessorChainBuilder builder);

    protected ServiceStatistics createStatistics()
    {
        return new ServiceStatistics(name);
    }

    public ServiceStatistics getStatistics()
    {
        return stats;
    }

    @Deprecated
    public void dispatchEvent(MuleEvent event) throws MuleException
    {
        messageProcessorChain.process(event);
    }

    @Deprecated
    public MuleEvent sendEvent(MuleEvent event) throws MuleException
    {
        return messageProcessorChain.process(event);
    }

    /**
     * @return the Mule descriptor name which is associated with the service
     */
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
    }


    // /////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    // /////////////////////////////////////////////////////////////////////////////////////////

    public Model getModel()
    {
        return model;
    }

    public void setModel(Model model)
    {
        this.model = model;
    }

    public MessagingExceptionHandler getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    public void setMessageSource(MessageSource inboundMessageSource)
    {
        if (messageSource instanceof ClusterizableMessageSource)
        {
            this.messageSource = new ClusterizableMessageSourceWrapper(muleContext, (ClusterizableMessageSource) inboundMessageSource, this);
        }
        else
        {
            this.messageSource = inboundMessageSource;
        }
    }

    public MessageProcessor getOutboundMessageProcessor()
    {
        return outboundRouter;
    }

    // TODO Use spring factory bean
    @Deprecated
    public void setMessageProcessor(MessageProcessor processor)
    {
        setOutboundMessageProcessor(processor);
    }
    
    public void setOutboundMessageProcessor(MessageProcessor processor)
    {
        this.outboundRouter = processor;
    }

    public ServiceAsyncReplyCompositeMessageSource getAsyncReplyMessageSource()
    {
        return asyncReplyMessageSource;
    }

    public void setAsyncReplyMessageSource(ServiceAsyncReplyCompositeMessageSource asyncReplyMessageSource)
    {
        this.asyncReplyMessageSource = asyncReplyMessageSource;
    }

    public String getInitialState()
    {
        return initialState;
    }

    public void setInitialState(String initialState)
    {
        this.initialState = initialState;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Component getComponent()
    {
        return component;
    }

    public void setComponent(Component component)
    {
        this.component = component;
        this.component.setFlowConstruct(this);
        if (component instanceof MuleContextAware)
        {
            ((MuleContextAware) component).setMuleContext(muleContext);

        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }
    
    public MessageInfoMapping getMessageInfoMapping()
    {
        return messageInfoMapping;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

    protected long getAsyncReplyTimeout()
    {
        if (asyncReplyMessageSource.getTimeout() != null)
        {
            return asyncReplyMessageSource.getTimeout().longValue();
        }
        else
        {
            return muleContext.getConfiguration().getDefaultResponseTimeout();
        }
    }

    protected ServiceAsyncRequestReplyRequestor createAsyncReplyProcessor()
    {
        ServiceAsyncRequestReplyRequestor asyncReplyMessageProcessor = new ServiceAsyncRequestReplyRequestor();
        asyncReplyMessageProcessor.setTimeout(getAsyncReplyTimeout());
        asyncReplyMessageProcessor.setFailOnTimeout(asyncReplyMessageSource.isFailOnTimeout());
        asyncReplyMessageProcessor.setReplySource(asyncReplyMessageSource);
        return asyncReplyMessageProcessor;
    }
    
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        final MuleEvent newEvent = new DefaultMuleEvent(event, this);
        RequestContext.setEvent(newEvent);
        try
        {
            ExecutionTemplate<MuleEvent> executionTemplate = ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate(muleContext, this.exceptionListener);
            return executionTemplate.execute(new ExecutionCallback<MuleEvent>() {

                @Override
                public MuleEvent process() throws Exception
                {
                    MuleEvent result = messageProcessorChain.process(newEvent);
                    if (result != null  && !VoidMuleEvent.getInstance().equals(result))
                    {
                        result.getMessage().release();
                        return new DefaultMuleEvent(result, event.getFlowConstruct());
                    }
                    return null;
                }
            });
        }
        catch (MessagingException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(CoreMessages.createStaticMessage("Flow execution exception"),e);
        }
        finally
        {
            RequestContext.setEvent(event);
            event.getMessage().release();
        }
    }

    public MessageProcessorChain getMessageProcessorChain()
    {
        return messageProcessorChain;
    }

    protected void injectFlowConstructMuleContextExceptionHandler(Object candidate)
    {
        injectFlowConstructMuleContext(candidate);
        if (candidate instanceof MessagingExceptionHandlerAware)
        {
            ((MessagingExceptionHandlerAware) candidate).setMessagingExceptionHandler(exceptionListener);
        }
    }
    
    protected void injectFlowConstructMuleContext(Object candidate)
    {
        if (candidate instanceof FlowConstructAware)
        {
            ((FlowConstructAware) candidate).setFlowConstruct(this);
        }
        if (candidate instanceof MuleContextAware)
        {
            ((MuleContextAware) candidate).setMuleContext(muleContext);
        }
    }

    protected void initialiseIfInitialisable(Object candidate) throws InitialisationException
    {
        if (candidate instanceof Initialisable)
        {
            ((Initialisable) candidate).initialise();
        }
    }

    protected void startIfStartable(Object candidate) throws MuleException
    {
        if (candidate instanceof Startable)
        {
            ((Startable) candidate).start();
        }
    }

    protected void stopIfStoppable(Object candidate) throws MuleException
    {
        if (candidate instanceof Stoppable)
        {
            ((Stoppable) candidate).stop();
        }
    }

    protected void disposeIfDisposable(Object candidate)
    {
        if (candidate instanceof Disposable)
        {
            ((Disposable) candidate).dispose();
        }
    }

    protected void pauseIfPausable(Object candidate) throws MuleException
    {
        if (candidate instanceof Pausable)
        {
            ((Pausable) candidate).pause();
        }
    }

    protected void resumeIfResumable(Object candidate) throws MuleException
    {
        if (candidate instanceof Resumable)
        {
            ((Resumable) candidate).resume();
        }
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
}
