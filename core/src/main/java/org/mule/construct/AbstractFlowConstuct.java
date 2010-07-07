/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.FlowConstruct;
import org.mule.api.FlowConstructAware;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.FlowConstructNotification;
import org.mule.context.notification.ServiceNotification;
import org.mule.lifecycle.AbstractLifecycleManager;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.util.ClassUtils;

public abstract class AbstractFlowConstuct implements FlowConstruct, Lifecycle
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected final MuleContext muleContext;

    protected final LifecycleManager lifecycleManager;

    protected ExceptionListener exceptionListener;

    protected String name;

    protected MessageSource inboundMessageSource;

    protected MessageProcessor messageProcessorChain;

    // temporary private implementation that only logs and fire notifications but
    // performs no particular action
    private static class NoActionLifecycleManager extends AbstractLifecycleManager
    {
        private final FlowConstruct flowConstruct;

        public NoActionLifecycleManager(FlowConstruct flowConstruct)
        {
            super(Integer.toString(flowConstruct.hashCode()));
            this.flowConstruct = flowConstruct;

            LifecycleManager muleLifecycleManager = flowConstruct.getMuleContext().getLifecycleManager();
            for (LifecyclePair pair : muleLifecycleManager.getLifecyclePairs())
            {
                registerLifecycle(pair);
            }
        }

        @Override
        protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
        {
            try
            {
                if (phase.getName().equals(Initialisable.PHASE_NAME))
                {
                    logger.debug("Initialising: " + flowConstruct.getName());
                    fireFlowConstructNotification(ServiceNotification.SERVICE_INITIALISED);
                }
                else if (phase.getName().equals(Startable.PHASE_NAME))
                {
                    logger.debug("Starting: " + flowConstruct.getName());
                    fireFlowConstructNotification(ServiceNotification.SERVICE_STARTED);
                }
                else if (phase.getName().equals(Stoppable.PHASE_NAME))
                {
                    logger.debug("Stopping: " + flowConstruct.getName());
                    fireFlowConstructNotification(ServiceNotification.SERVICE_STOPPED);
                }
                else if (phase.getName().equals(Disposable.PHASE_NAME))
                {
                    logger.debug("Disposing: " + flowConstruct.getName());
                    fireFlowConstructNotification(ServiceNotification.SERVICE_DISPOSED);
                }
                else
                {
                    throw new LifecycleException(CoreMessages.lifecyclePhaseNotRecognised(phase.getName()),
                        flowConstruct);
                }
            }
            catch (MuleException e)
            {
                throw new LifecycleException(e, flowConstruct);
            }
        }

        protected void fireFlowConstructNotification(int action)
        {
            flowConstruct.getMuleContext().fireNotification(
                new FlowConstructNotification(flowConstruct, action));
        }
    }

    public AbstractFlowConstuct(MuleContext muleContext, String name) throws MuleException
    {
        this.muleContext = muleContext;
        this.lifecycleManager = new NoActionLifecycleManager(this);
    }

    public final void initialise() throws InitialisationException
    {
        try
        {
            createMessageProcessorChain();

            if (inboundMessageSource != null)
            {
                inboundMessageSource.setListener(messageProcessorChain);
            }

            injectFlowConstructMuleContext(inboundMessageSource);
            injectFlowConstructMuleContext(messageProcessorChain);
            initialiseObject(inboundMessageSource);
            initialiseObject(messageProcessorChain);
            doInitialise();
            validateConstuct();
            lifecycleManager.fireLifecycle(Initialisable.PHASE_NAME);
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public final void start() throws MuleException
    {
        lifecycleManager.checkPhase(Startable.PHASE_NAME);
        startObject(messageProcessorChain);
        startObject(inboundMessageSource);
        doStart();
        lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
    }

    public final void stop() throws MuleException
    {
        lifecycleManager.checkPhase(Stoppable.PHASE_NAME);
        stopObject(messageProcessorChain);
        stopObject(inboundMessageSource);
        doStop();
        lifecycleManager.fireLifecycle(Stoppable.PHASE_NAME);
    }

    public final void dispose()
    {
        try
        {
            disposeObject(messageProcessorChain);
            disposeObject(inboundMessageSource);
            doDispose();
            lifecycleManager.fireLifecycle(Disposable.PHASE_NAME);
        }
        catch (MuleException me)
        {
            logger.error("Failed to stop: " + this.toString(), me);
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

    protected void createMessageProcessorChain()
    {
        ChainMessageProcessorBuilder builder = new ChainMessageProcessorBuilder();
        configureMessageProcessors(builder);
        messageProcessorChain = builder.build();
    }

    protected abstract void configureMessageProcessors(ChainMessageProcessorBuilder builder);

    public String getName()
    {
        return name;
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    protected void doInitialise() throws InitialisationException
    {
        // Empty template method
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
        // Empty template method
    }

    protected void validateConstuct() throws InitialisationException
    {
        // Empty template method
    }

    private void injectFlowConstructMuleContext(Object candidate)
    {
        if (candidate instanceof FlowConstructAware)
        {
            ((FlowConstructAware) candidate).setFlowConstruct(this);
        }
        if (messageProcessorChain instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageProcessorChain).setFlowConstruct(this);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
    }

    private void initialiseObject(Object candidate) throws InitialisationException
    {
        if (candidate instanceof Initialisable)
        {
            ((Initialisable) candidate).initialise();
        }
    }

    private void startObject(Object candidate) throws MuleException
    {
        if (candidate instanceof Startable)
        {
            ((Startable) candidate).start();
        }
    }

    private void stopObject(Object candidate) throws MuleException
    {
        if (candidate instanceof Stoppable)
        {
            ((Stoppable) candidate).stop();
        }
    }

    private void disposeObject(Object candidate) throws InitialisationException
    {
        if (candidate instanceof Disposable)
        {
            ((Disposable) candidate).dispose();
        }
    }

}
