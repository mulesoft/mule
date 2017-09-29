/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.List;
import java.util.function.BiFunction;

import org.slf4j.Logger;

/**
 * Component to be used that supports a collection of {@link MessageProcessorChain} and executes them in sequentially waiting for
 * each chain and all child context executions (such as async blocks) to complete.
 * <p/>
 * Meant to be used by runtime privileged extensions that need to construct top level chains of execution.
 *
 * @since 4.0
 */
public class CompositeProcessorChainRouter extends AbstractExecutableComponent implements Lifecycle {

  private static Logger LOGGER = getLogger(CompositeProcessorChainRouter.class);

  private List<MessageProcessorChain> processorChains = emptyList();

  public void setProcessorChains(List<MessageProcessorChain> processorChains) {
    this.processorChains = processorChains;
  }

  @Override
  protected ReactiveProcessor getExecutableFunction() {
    return publisher -> from(publisher).flatMapMany(initial -> fromIterable(processorChains).reduce(initial, processChain()));
  }

  private BiFunction<CoreEvent, MessageProcessorChain, CoreEvent> processChain() {
    return (event, processorChain) -> {
      BaseEventContext childContext = child((BaseEventContext) event.getContext(), ofNullable(getLocation()));
      CoreEvent result = from(processWithChildContext(event, processorChain, childContext)).block();
      // Block until all child contexts are complete
      from(childContext.getCompletionPublisher()).block();
      return result;
    };
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(processorChains);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(processorChains, LOGGER);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processorChains);
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    setMuleContextIfNeeded(processorChains, muleContext);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(processorChains, muleContext);
  }
}
