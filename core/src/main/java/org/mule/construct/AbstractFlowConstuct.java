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

import org.mule.api.FlowConstruct;
import org.mule.api.FlowConstructAware;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFlowConstuct implements FlowConstruct, Lifecycle, LifecycleStateEnabled
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected final MuleContext muleContext;

    protected final FlowConstructLifecycleManager lifecycleManager;

    protected ExceptionListener exceptionListener;

    protected String name;

    protected MessageSource inboundMessageSource;

    protected MessageProcessor messageProcessorChain;

    public AbstractFlowConstuct(MuleContext muleContext, String name) throws MuleException
    {
        this.muleContext = muleContext;
        this.name = name;
        this.lifecycleManager = new FlowConstructLifecycleManager(this);
    }


    public final void initialise() throws InitialisationException
    {
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<FlowConstruct>()
            {
                public void onTransition(String phaseName, FlowConstruct object) throws MuleException
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
        lifecycleManager.fireStartPhase(new LifecycleCallback<FlowConstruct>()
        {
            public void onTransition(String phaseName, FlowConstruct object) throws MuleException
            {
                startObject(messageProcessorChain);
                startObject(inboundMessageSource);
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
                stopObject(inboundMessageSource);
                stopObject(messageProcessorChain);
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
                    disposeObject(messageProcessorChain);
                    disposeObject(inboundMessageSource);
                    doDispose();
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

    private void disposeObject(Object candidate)
    {
        if (candidate instanceof Disposable)
        {
            ((Disposable) candidate).dispose();
        }
    }

}
