/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation of {@link FlowConstruct} that:
 * <ul>
 *  <li>Is constructed with unique name and {@link MuleContext}.
 *  <li>Uses a {@link MessageSource} as the source of messages.
 *  <li>Uses a chain of {@link MessageProcessor}s to process messages.
 *  <li>Has lifecycle and propagates this lifecycle to both {@link MessageSource} and
 *  {@link MessageProcessor}s in the correct order depending on the lifecycle phase.
 *  <li>Allows an {@link ExceptionListener} to be set.
 * </ul>
 * Implementations of <code>AbstractFlowConstuct</code> should implement
 * {@link #configureMessageProcessors(org.mule.api.processor.MessageProcessorChainBuilder)} and
 * {@link #validateConstruct()} to construct the processing chain required and
 * validate the resulting construct. Validation may include validation of the type of
 * attributes of the {@link MessageSource}.
 * <p/>
 * Implementations may also implement {@link #doInitialise()}, {@link #doStart()},
 * {@link #doStop()} and {@link #doDispose()} if they need to perform any action on
 * lifecycle transitions.
 */
public abstract class AbstractFlowConstruct implements FlowConstruct, Lifecycle
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected String name;
    protected MessageSource messageSource;
    protected MessageProcessorChain messageProcessorChain;
    protected MessagingExceptionHandler exceptionListener;
    protected final FlowConstructLifecycleManager lifecycleManager;
    protected final MuleContext muleContext;
    protected FlowConstructStatistics statistics;
    protected MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();
    protected ThreadingProfile threadingProfile;
    private boolean canProcessMessage = false;
    
    /**
     * The initial states that the flow can be started in
     */
    public static final String INITIAL_STATE_STOPPED = "stopped";
    public static final String INITIAL_STATE_STARTED = "started";
    
    /**
     * Determines the initial state of this flow when the mule starts. Can be
     * 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    public AbstractFlowConstruct(String name, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.name = name;
        this.lifecycleManager = new FlowConstructLifecycleManager(this, muleContext);
        this.exceptionListener = new DefaultServiceExceptionStrategy(muleContext);
    }

    public final void initialise() throws InitialisationException
    {
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    createMessageProcessor();

                    if (messageSource != null)
                    {
                        // Wrap chain to decouple lifecycle
                        messageSource.setListener(new AbstractInterceptingMessageProcessor()
                        {
                            public MuleEvent process(MuleEvent event) throws MuleException
                            {
                                return messageProcessorChain.process(event);
                            }
                        });
                    }

                    injectFlowConstructMuleContext(messageSource);
                    injectFlowConstructMuleContext(messageProcessorChain);
                    injectFlowConstructMuleContext(exceptionListener);
                    initialiseIfInitialisable(messageSource);
                    initialiseIfInitialisable(messageProcessorChain);
                    initialiseIfInitialisable(exceptionListener);

                    doInitialise();

                    validateConstruct();
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

    public final void start() throws MuleException
    {
        // Check if Initial State is Stopped
        if(!isStopped() && initialState.equals(INITIAL_STATE_STOPPED))
        {
            lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<FlowConstruct>());
            lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<FlowConstruct>());

            logger.info("Flow " + name + " has not been started (initial state = 'stopped')");
            return;
        }
        
        lifecycleManager.fireStartPhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                startIfStartable(messageProcessorChain);
                startIfStartable(exceptionListener);
                canProcessMessage = true;
                startIfStartable(messageSource);
                doStart();
            }
        });
    }

    public final void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                try
                {
                    stopIfStoppable(messageSource);
                }
                finally
                {
                    canProcessMessage = false;
                }
                stopIfStoppable(messageProcessorChain);
                stopIfStoppable(exceptionListener);
                doStop();
            }
        });
    }

    public final void dispose()
    {
        try
        {
            if (isStarted())
            {
                stop();
            }

            lifecycleManager.fireDisposePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    disposeIfDisposable(messageProcessorChain);
                    disposeIfDisposable(exceptionListener);
                    disposeIfDisposable(messageSource);
                    doDispose();
                }
            });
        }
        catch (MuleException e)
        {
            logger.error("Failed to stop service: " + name, e);
        }
    }

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }

    public boolean isStarted()
    {
        return lifecycleManager.getState().isStarted();
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
     * Creates a {@link MessageProcessor} that will process messages from the
     * configured {@link MessageSource}.
     * <p>
     * The default implementation of this methods uses a
     * {@link DefaultMessageProcessorChainBuilder} and allows a chain of
     * {@link MessageProcessor}s to be configured using the
     * {@link #configureMessageProcessors(org.mule.api.processor.MessageProcessorChainBuilder)}
     * method but if you wish to use another {@link MessageProcessorBuilder} or just
     * a single {@link MessageProcessor} then this method can be overridden and
     * return a single {@link MessageProcessor} instead.
     * 
     * @throws MuleException
     */
    protected void createMessageProcessor() throws MuleException
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(this);
        builder.setName("'" + getName() + "' processor chain");
        configureMessageProcessors(builder);
        messageProcessorChain = builder.build();
    }

    /**
     * Used to configure the processing chain for this <code>FlowConstuct</code by
     * adding {@link MessageProcessor}s to the chain using the builder provided.
     * <p>
     * To use a different builder of to construct a composite
     * {@link MessageProcessor} manually override {@link #createMessageProcessor()}
     * instead.
     * 
     *
     *
     *
     * @param builder instance of {@link org.mule.processor.chain.DefaultMessageProcessorChainBuilder}
     * @throws MuleException
     */
    protected abstract void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException;

    public String getName()
    {
        return name;
    }

    public MessagingExceptionHandler getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }
    
    public String getInitialState()
    {
        return initialState;
    }

    public void setInitialState(String initialState)
    {
        this.initialState = initialState;
    }    

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    public void setMessageSource(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    public FlowConstructStatistics getStatistics()
    {
        return statistics;
    }

    public MessageInfoMapping getMessageInfoMapping()
    {
        return messageInfoMapping;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

    protected void doInitialise() throws InitialisationException
    {
        int threadPoolSize = threadingProfile == null ? 0 : threadingProfile.getMaxThreadsActive();
        statistics = new FlowConstructStatistics(getConstructType(), name, threadPoolSize);
        statistics.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(statistics);
    }

    protected void doStart() throws MuleException
    {
        // Empty template method
    }

    protected void doStop() throws MuleException
    {
        // Empty template method
    }

    protected void doDispose()
    {
        muleContext.getStatistics().remove(statistics);
    }

    /**
     * Validates configured flow construct
     * 
     * @throws FlowConstructInvalidException if the flow construct does not pass
     *             validation
     */
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        // Empty template method
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

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
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
    
    public MessageProcessorChain getMessageProcessorChain()
    {
        return this.messageProcessorChain;
    }

    /**
     * @return the type of construct being created, e.g. "Flow"
     */
    public abstract String getConstructType();

    public class ProcessIfPipelineStartedMessageProcessor extends AbstractFilteringMessageProcessor
    {

        @Override
        protected boolean accept(MuleEvent event)
        {
            return canProcessMessage;
        }

        @Override
        protected MuleEvent handleUnaccepted(MuleEvent event) throws LifecycleException
        {
            throw new LifecycleException(CoreMessages.isStopped(getName()), event.getMessage());
        }
    }
}
