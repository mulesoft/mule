/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
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
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.source.MessageSource;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.lifecycle.processor.ProcessIfStartedWaitIfPausedMessageProcessor;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.routing.AbstractRouterCollection;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.response.DefaultResponseRouterCollection;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base implementation for all Services in Mule
 */
public abstract class AbstractService implements Service
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
    protected ExceptionListener exceptionListener;

    /**
     * The service's name
     */
    protected String name;

    protected OutboundRouterCollection outboundRouter = new DefaultOutboundRouterCollection();

    protected ResponseRouterCollection responseRouter = new DefaultResponseRouterCollection();

    protected ServiceCompositeMessageSource messageSource = new ServiceCompositeMessageSource();
    protected MessageSource asyncReplyMessageSource;

    protected MessageProcessor messageProcessorChain;

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
        try
        {
            lifecycleManager = new ServiceLifecycleManager(this);
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
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    ((MuleContextAware) outboundRouter).setMuleContext(muleContext);

                    if (exceptionListener == null)
                    {
                        // By default us the model Exception Listener
                        // TODO MULE-2102 This should be configured in the default template.
                        exceptionListener = getModel().getExceptionListener();
                    }

                    asyncReplyMessageSource = (((DefaultInboundRouterCollection) responseRouter).getMessageSource());
                    if (messageSource instanceof FlowConstructAware)
                    {
                        ((FlowConstructAware) messageSource).setFlowConstruct(object);
                    }
                    if (asyncReplyMessageSource instanceof FlowConstructAware)
                    {
                        ((FlowConstructAware) asyncReplyMessageSource).setFlowConstruct(object);
                    }
                    // Ensure Component has service instance and is initialised. If the component
                    // was configured with spring and is therefore in the registry it will get
                    // started automatically, if it was set on the service directly then it won't
                    // be started automatically. So to be sure we start it here.
                    component.setFlowConstruct(object);

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
            if(isStarted())
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
     * Custom components can execute code necessary to put the service in a paused
     * state here. If a developer overloads this method the doResume() method MUST
     * also be overloaded to avoid inconsistent state in the service
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
        if (messageSource instanceof Stoppable)
        {
            ((Stoppable) messageSource).stop();
        }
        if (asyncReplyMessageSource instanceof Stoppable)
        {
            ((Stoppable) asyncReplyMessageSource).stop();
        }

        // Component is not in chain
        if (component instanceof Stoppable)
        {
            ((Stoppable) component).stop();
        }
        if (messageProcessorChain instanceof Stoppable)
        {
            ((Stoppable) messageProcessorChain).stop();
        }
    }

    protected void doStart() throws MuleException
    {
        // Component is not in chain
        if (component instanceof Startable)
        {
            ((Startable) component).start();
        }
        if (messageProcessorChain instanceof Startable)
        {
            ((Startable) messageProcessorChain).start();
        }

        if (messageSource instanceof Startable)
        {
            ((Startable) messageSource).start();
        }
        if (asyncReplyMessageSource instanceof Startable)
        {
            ((Startable) asyncReplyMessageSource).start();
        }
    }

    protected void doDispose()
    {
        // Component is not in chain
        if (component instanceof Disposable)
        {
            ((Disposable) component).dispose();
        }
        if (messageProcessorChain instanceof Disposable)
        {
            ((Disposable) messageProcessorChain).dispose();
        }
        responseRouter.dispose();
        muleContext.getStatistics().remove(stats);
    }

    protected void doInitialise() throws InitialisationException
    {
        // initialise statistics
        stats = createStatistics();
        stats.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(stats);
        if (outboundRouter != null)
        {
            stats.setOutboundRouterStat(outboundRouter.getStatistics());
        }
        stats.setInboundRouterStat(messageSource.getStatistics());
        stats.setComponentStat(component.getStatistics());

        buildServiceMessageProcessorChain();
        messageSource.setListener(messageProcessorChain);
        asyncReplyMessageSource.setListener(getResponseRouter());

        // Component is not in chain
        if (component instanceof Initialisable)
        {
            ((Initialisable) component).initialise();
        }
        if (messageProcessorChain instanceof Initialisable)
        {
            ((Initialisable) messageProcessorChain).initialise();
        }
        ((AbstractRouterCollection) responseRouter).setMuleContext(muleContext);
        responseRouter.initialise();
        messageSource.initialise();
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

    protected void handleException(Exception e)
    {
        exceptionListener.exceptionThrown(e);
    }

    protected void buildServiceMessageProcessorChain()
    {
        InterceptingChainMessageProcessorBuilder builder = new InterceptingChainMessageProcessorBuilder();
        builder.setName("Service '"+name+"' Processor Chain");
        builder.chain(getServiceStartedAssertingMessageProcessor());
        addMessageProcessors(builder);
        messageProcessorChain = builder.build();

        if (messageProcessorChain instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageProcessorChain).setFlowConstruct(this);
        }
    }

    protected MessageProcessor getServiceStartedAssertingMessageProcessor()
    {
        return new ProcessIfStartedWaitIfPausedMessageProcessor(this, lifecycleManager.getState());
    }

    protected abstract void addMessageProcessors(InterceptingChainMessageProcessorBuilder builder);

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
    public MuleMessage sendEvent(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent = messageProcessorChain.process(event);
        if (resultEvent != null)
        {
            return resultEvent.getMessage();
        }
        else
        {
            return null;
        }
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

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public ServiceCompositeMessageSource getMessageSource()
    {
        return messageSource;
    }

    public void setMessageSource(ServiceCompositeMessageSource inboundMessageSource)
    {
        this.messageSource = inboundMessageSource;
    }

    public OutboundRouterCollection getOutboundRouter()
    {
        return outboundRouter;
    }

    public void setOutboundRouter(OutboundRouterCollection outboundRouter)
    {
        this.outboundRouter = outboundRouter;
    }

    public ResponseRouterCollection getResponseRouter()
    {
        return responseRouter;
    }

    public void setResponseRouter(ResponseRouterCollection responseRouter)
    {
        this.responseRouter = responseRouter;
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
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

}
