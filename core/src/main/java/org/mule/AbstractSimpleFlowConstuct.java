/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

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
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.lifecycle.AbstractLifecycleManager;
import org.mule.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.util.ClassUtils;

public abstract class AbstractSimpleFlowConstuct implements FlowConstruct, Lifecycle
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected final MuleContext muleContext;

    protected final LifecycleManager lifecycleManager;

    protected ExceptionListener exceptionListener;

    protected String name;

    protected MessageSource inboundMessageSource;

    protected MessageProcessor messageProcessorChain;

    private static class NoActionLifecycleManager extends AbstractLifecycleManager
    {
        public NoActionLifecycleManager(String id)
        {
            super(id);
        }

        @Override
        protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
        {
            // NOOP
        }
    }

    public AbstractSimpleFlowConstuct(MuleContext muleContext)
    {
        this.muleContext = muleContext;

        // name hasn't be set at this point so use hashcode asn ID
        this.lifecycleManager = new NoActionLifecycleManager(Integer.toString(this.hashCode()));
    }

    protected void buildServiceMessageProcessorChain()
    {
        ChainMessageProcessorBuilder builder = new ChainMessageProcessorBuilder();
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
        return new ProcessIfStartedMessageProcessor(this, getLifecycleState());
    }

    protected abstract void addMessageProcessors(ChainMessageProcessorBuilder builder);

    public void initialise() throws InitialisationException
    {
        // TODO add support for statistics

        if (inboundMessageSource instanceof FlowConstructAware)
        {
            ((FlowConstructAware) inboundMessageSource).setFlowConstruct(this);
        }

        buildServiceMessageProcessorChain();

        try
        {
            lifecycleManager.fireLifecycle(Initialisable.PHASE_NAME);
        }
        catch (LifecycleException le)
        {
            throw new InitialisationException(le, this);
        }
    }

    public void start() throws MuleException
    {
        lifecycleManager.checkPhase(Startable.PHASE_NAME);

        if (messageProcessorChain instanceof Startable)
        {
            ((Startable) messageProcessorChain).start();
        }

        if (inboundMessageSource instanceof Startable)
        {
            ((Startable) inboundMessageSource).start();
        }

        lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
    }

    public void stop() throws MuleException
    {
        if (inboundMessageSource instanceof Stoppable)
        {
            ((Stoppable) inboundMessageSource).stop();
        }

        if (messageProcessorChain instanceof Stoppable)
        {
            ((Stoppable) messageProcessorChain).stop();
        }

        lifecycleManager.fireLifecycle(Stoppable.PHASE_NAME);
    }

    public void dispose()
    {
        try
        {
            lifecycleManager.fireLifecycle(Disposable.PHASE_NAME);
        }
        catch (MuleException me)
        {
            logger.error("Failed to stop: " + this.toString(), me);
        }
    }

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setInboundMessageSource(MessageSource inboundMessageSource)
    {
        this.inboundMessageSource = inboundMessageSource;
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
    }

}
