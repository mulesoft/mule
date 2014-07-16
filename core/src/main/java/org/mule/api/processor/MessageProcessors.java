/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.processor.chain.DefaultMessageProcessorChain;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors
{

    private MessageProcessors()
    {
        // do not instantiate
    }

    public static MessageProcessorChain singletonChain(MessageProcessor mp)
    {
        return DefaultMessageProcessorChain.from(mp);
    }

    public static MessageProcessor lifecyleAwareMessageProcessorWrapper(final MessageProcessor mp)
    {
        return new LifecyleAwareMessageProcessorWrapper(mp);
    }

    private static class LifecyleAwareMessageProcessorWrapper
            implements MessageProcessor, Lifecycle, MuleContextAware, FlowConstructAware
    {

        private MessageProcessor delegate;


        public LifecyleAwareMessageProcessorWrapper(MessageProcessor delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            if (delegate instanceof Initialisable)
            {
                ((Initialisable) delegate).initialise();
            }
        }

        @Override
        public void start() throws MuleException
        {
            if (delegate instanceof Startable)
            {
                ((Startable) delegate).start();
            }
        }

        @Override
        public void stop() throws MuleException
        {
            if (delegate instanceof Stoppable)
            {
                ((Stoppable) delegate).stop();
            }
        }

        @Override
        public void dispose()
        {
            if (delegate instanceof Disposable)
            {
                ((Disposable) delegate).dispose();
            }
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            if (delegate instanceof FlowConstructAware)
            {
                ((FlowConstructAware) delegate).setFlowConstruct(flowConstruct);
            }
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            if (delegate instanceof MuleContextAware)
            {
                ((MuleContextAware) delegate).setMuleContext(context);
            }
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return delegate.process(event);
        }
    }
}
