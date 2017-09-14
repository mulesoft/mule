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
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Component to be used that supports a collection of processors and executes them in order.
 * <p/>
 * Meant to be used be runtime privileged extensions that needs to construct top level chains of execution.
 *
 * @since 4.0
 */
public class ProcessorChainRouter extends AbstractExecutableComponent implements Lifecycle {

  private static Logger LOGGER = getLogger(ProcessorChainRouter.class);

  @Inject
  private MuleContext muleContext;

  private String name;
  private List<Processor> processors = emptyList();
  private MessageProcessorChain processorChain;

  @Override
  protected ReactiveProcessor getExecutableFunction() {
    return publisher -> from(publisher).transform(processorChain);
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
    startIfNeeded(processorChain);
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
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
    initialiseIfNeeded(processorChain, muleContext);
  }
}

