/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors {

  private MessageProcessors() {
    // do not instantiate
  }

  public static MessageProcessorChain singletonChain(MessageProcessor mp) {
    return DefaultMessageProcessorChain.from(mp);
  }

  public static MessageProcessor lifecyleAwareMessageProcessorWrapper(final MessageProcessor mp) {
    return new LifecyleAwareMessageProcessorWrapper(mp);
  }

  private static class LifecyleAwareMessageProcessorWrapper
      implements MessageProcessor, Lifecycle, MuleContextAware, FlowConstructAware {

    private MessageProcessor delegate;


    public LifecyleAwareMessageProcessorWrapper(MessageProcessor delegate) {
      this.delegate = delegate;
    }

    @Override
    public void initialise() throws InitialisationException {
      if (delegate instanceof Initialisable) {
        ((Initialisable) delegate).initialise();
      }
    }

    @Override
    public void start() throws MuleException {
      if (delegate instanceof Startable) {
        ((Startable) delegate).start();
      }
    }

    @Override
    public void stop() throws MuleException {
      if (delegate instanceof Stoppable) {
        ((Stoppable) delegate).stop();
      }
    }

    @Override
    public void dispose() {
      if (delegate instanceof Disposable) {
        ((Disposable) delegate).dispose();
      }
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      if (delegate instanceof FlowConstructAware) {
        ((FlowConstructAware) delegate).setFlowConstruct(flowConstruct);
      }
    }

    @Override
    public void setMuleContext(MuleContext context) {
      if (delegate instanceof MuleContextAware) {
        ((MuleContextAware) delegate).setMuleContext(context);
      }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      return delegate.process(event);
    }
  }
}
