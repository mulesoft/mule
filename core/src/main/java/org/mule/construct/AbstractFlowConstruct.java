/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

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
 * {@link #validateConstruct()} validate the resulting construct. Validation may 
 * include validation of the type of attributes of the {@link MessageSource}.
 * <p/>
 * Implementations may also implement {@link #doInitialise()}, {@link #doStart()},
 * {@link #doStop()} and {@link #doDispose()} if they need to perform any action on
 * lifecycle transitions.
 */
public abstract class AbstractFlowConstruct implements FlowConstruct, Lifecycle, AnnotatedObject
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected String name;
    protected MessagingExceptionHandler exceptionListener;
    protected final FlowConstructLifecycleManager lifecycleManager;
    protected final MuleContext muleContext;
    protected FlowConstructStatistics statistics;
    protected MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();
    
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
    }

    public final void initialise() throws InitialisationException
    {
        try
        {
            if (exceptionListener == null)
            {
                this.exceptionListener = muleContext.getDefaultExceptionStrategy();
            }
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
                {
                    injectFlowConstructMuleContext(exceptionListener);
                    initialiseIfInitialisable(exceptionListener);
                    validateConstruct();
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
                startIfStartable(exceptionListener);
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
                doStop();
                stopIfStoppable(exceptionListener);
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
                    doDispose();
                    disposeIfDisposable(exceptionListener);
                }
            });
        }
        catch (MuleException e)
        {
            logger.error("Failed to stop service: " + name, e);
        }
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
    
    protected void doInitialise() throws MuleException
    {
        configureStatistics();
    }

    protected void configureStatistics()
    {
        statistics = new FlowConstructStatistics(getConstructType(), name);
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
        if (exceptionListener instanceof MessagingExceptionHandlerAcceptor)
        {
            if (!((MessagingExceptionHandlerAcceptor)exceptionListener).acceptsAll())
            {
                throw new FlowConstructInvalidException(CoreMessages.createStaticMessage("Flow exception listener contains and exception strategy that doesn't handle all request," +
                        " Perhaps there's an exception strategy with a when attribute set but it's not part of a catch exception strategy"),this);
            }
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

    protected void injectExceptionHandler(Object candidate)
    {
        if (candidate instanceof MessagingExceptionHandlerAware)
        {
            ((MessagingExceptionHandlerAware) candidate).setMessagingExceptionHandler(this.getExceptionListener());
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

    /**
     * @return the type of construct being created, e.g. "Flow"
     */
    public abstract String getConstructType();
}
