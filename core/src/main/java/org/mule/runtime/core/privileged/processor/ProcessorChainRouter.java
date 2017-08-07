/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.util.Collections.emptyList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.chain.ExplicitMessageProcessorChainBuilder;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component to be used that supports a collection of processors and executes them in order.
 * <p/>
 * Meant to be used be runtime privileged extensions that needs to construct top level chains of execution.
 *
 * @since 4.0
 */
public class ProcessorChainRouter extends AbstractAnnotatedObject implements Lifecycle {

  private static Logger LOGGER = LoggerFactory.getLogger(CompositeProcessorChainRouter.class);

  @Inject
  private MuleContext muleContext;

  private String name;
  private List<Processor> processors = emptyList();
  private MessageProcessorChain processorChain;

  public Event process(Event event) {
    org.mule.runtime.core.api.Event.Builder builder =
        org.mule.runtime.core.api.Event.builder(DefaultEventContext.create(getUUID(), muleContext.getId(), getLocation()));
    org.mule.runtime.core.api.Event defaultEvent = builder.from(event).build();
    try {
      return processorChain.process(defaultEvent);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public void setProcessors(List processors) {
    this.processors = processors;
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(processorChain);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(processorChain, LOGGER);
  }

  @Override
  public void start() throws MuleException {
    LifecycleUtils.startIfNeeded(processorChain);
  }

  @Override
  public void initialise() throws InitialisationException {
    ExplicitMessageProcessorChainBuilder builder = new ExplicitMessageProcessorChainBuilder();
    builder.setName("processor chain '" + name + "'");
    for (Object processor : processors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
    processorChain = builder.build();
    processorChain.setMuleContext(muleContext);
    initialiseIfNeeded(processorChain, muleContext);
  }
}

