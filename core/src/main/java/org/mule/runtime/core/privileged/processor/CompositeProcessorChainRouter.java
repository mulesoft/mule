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
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component to be used that supports a collection of {@link MessageProcessorChain} and executes them in order.
 * <p/>
 * Meant to be used by runtime privileged extensions that need to construct top level chains of execution.
 * 
 * @since 4.0
 */
public class CompositeProcessorChainRouter extends AbstractExecutableComponent implements Lifecycle {

  private static Logger LOGGER = LoggerFactory.getLogger(CompositeProcessorChainRouter.class);

  @Inject
  private MuleContext muleContext;

  private String name;
  private List<MessageProcessorChain> processorChains = emptyList();
  private MessageProcessorChain messageProcessorChain;

  public void setProcessorChains(List processorChains) {
    this.processorChains = processorChains;
  }

  @Override
  protected Function<Publisher<InternalEvent>, Publisher<InternalEvent>> getExecutableFunction() {
    return publisher -> from(publisher).transform(messageProcessorChain);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(messageProcessorChain);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(messageProcessorChain, LOGGER);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(messageProcessorChain);
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
    for (MessageProcessorChain processorChain : processorChains) {
      chainBuilder.chain(processorChain);
    }
    messageProcessorChain = chainBuilder.build();
    initialiseIfNeeded(messageProcessorChain, muleContext);
  }
}
