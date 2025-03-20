/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.HasLocation;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.profiling.tracing.ComponentTracerAware;
import org.mule.runtime.tracer.api.component.ComponentTracer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Constructs a chain of {@link Processor}s and wraps the invocation of the chain in a composite MessageProcessor.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
@NoExtend
public class DefaultMessageProcessorChainBuilder extends AbstractMessageProcessorChainBuilder
    implements ComponentTracerAware<CoreEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageProcessorChainBuilder.class);

  /**
   * We iterate over the list of message processor to be chained together in reverse order collecting up those that can be
   * iterated over in a temporary list.
   */
  @Override
  public MessageProcessorChain build() {
    LinkedList<Processor> tempList = new LinkedList<>();

    // Start from last but one message processor and work backwards
    for (int i = processors.size() - 1; i >= 0; i--) {
      tempList.addFirst(processors.get(i));
    }

    return createSimpleChain(tempList, ofNullable(processingStrategy));
  }

  protected MessageProcessorChain createSimpleChain(List<Processor> tempList,
                                                    Optional<ProcessingStrategy> processingStrategyOptional) {
    DefaultMessageProcessorChain messageProcessorChain;

    if (tempList.size() == 1 && tempList.get(0) instanceof DefaultMessageProcessorChain chain) {
      messageProcessorChain = chain;
    } else {
      messageProcessorChain =
          new DefaultMessageProcessorChain(name != null ? "(chain) of " + name : "(chain)",
                                           processingStrategyOptional,
                                           new ArrayList<>(tempList),
                                           messagingExceptionHandler,
                                           location);
    }

    if (chainComponentTracer != null) {
      messageProcessorChain.setComponentTracer(chainComponentTracer);
    }

    return messageProcessorChain;
  }

  @Override
  public DefaultMessageProcessorChainBuilder chain(Processor... processors) {
    for (Processor messageProcessor : processors) {
      this.processors.add(messageProcessor);
    }
    return this;
  }

  public DefaultMessageProcessorChainBuilder chain(List<Processor> processors) {
    if (processors != null) {
      this.processors.addAll(processors);
    }
    return this;
  }

  @Override
  public void setComponentTracer(ComponentTracer<CoreEvent> componentTracer) {
    this.chainComponentTracer = componentTracer;
  }

  @NoExtend
  protected static class DefaultMessageProcessorChain extends AbstractMessageProcessorChain implements HasLocation {

    private ComponentLocation pipeLineLocation;

    protected DefaultMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                           List<Processor> processors,
                                           FlowExceptionHandler messagingExceptionHandler) {
      this(name, processingStrategyOptional, processors, messagingExceptionHandler, null);
    }

    protected DefaultMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                           List<Processor> processors,
                                           FlowExceptionHandler messagingExceptionHandler,
                                           ComponentLocation pipeLineLocation) {
      super(name, processingStrategyOptional, processors, messagingExceptionHandler);
      this.pipeLineLocation = pipeLineLocation;
    }

    /**
     * This constructor left for backwards compatibility
     *
     * @deprecated Use {@link DefaultMessageProcessorChainBuilder(String, Optional, List, FlowExceptionHandler)} instead.
     */
    @Deprecated
    protected DefaultMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional, Processor head,
                                           List<Processor> processors,
                                           List<Processor> processorsForLifecycle) {
      super(name, processingStrategyOptional, processors,
            // just let the error be propagated to the outer chain...
            (exception, event) -> null);
    }

    @Override
    public ComponentLocation resolveLocation() {
      return pipeLineLocation;
    }
  }

  /**
   * Helper method to create a lazy processor from a chain builder so the chain builder can get access to a
   * {@link FlowConstruct}{@link ProcessingStrategy}.
   *
   * @param chainBuilder               the chain builder
   * @param muleContext                the context
   * @param processingStrategySupplier a supplier of the processing strategy.
   * @return a lazy processor that will build the chain upon the first request.
   */
  public static MessageProcessorChain newLazyProcessorChainBuilder(AbstractMessageProcessorChainBuilder chainBuilder,
                                                                   MuleContext muleContext,
                                                                   Supplier<ProcessingStrategy> processingStrategySupplier) {
    return new LazyProcessorChainBuilder(chainBuilder.name, empty(), chainBuilder.processors, chainBuilder,
                                         processingStrategySupplier);
  }

  public interface MessagingExceptionHandlerAware {

    void setMessagingExceptionHandler(FlowExceptionHandler messagingExceptionHandler);
  }

  private static final class LazyProcessorChainBuilder extends AbstractMessageProcessorChain
      implements MessagingExceptionHandlerAware, ComponentTracerAware<CoreEvent> {

    private final AbstractMessageProcessorChainBuilder chainBuilder;
    private final Supplier<ProcessingStrategy> processingStrategySupplier;
    private FlowExceptionHandler messagingExceptionHandler;
    private MessageProcessorChain delegate;
    private ComponentTracer<CoreEvent> chainComponentTracer;

    private LazyProcessorChainBuilder(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                      List<Processor> processors,
                                      AbstractMessageProcessorChainBuilder chainBuilder,
                                      Supplier<ProcessingStrategy> processingStrategySupplier) {
      super(name, processingStrategyOptional, processors, null);
      this.chainBuilder = chainBuilder;
      this.processingStrategySupplier = processingStrategySupplier;
    }

    @Override
    public void initialise() throws InitialisationException {
      chainBuilder.setProcessingStrategy(processingStrategySupplier.get());
      chainBuilder.setMessagingExceptionHandler(messagingExceptionHandler);
      chainBuilder.setComponentTracer(chainComponentTracer);
      delegate = chainBuilder.build();
      delegate.setAnnotations(getAnnotations());
      initialiseIfNeeded(delegate, muleContext);
    }

    @Override
    public void start() throws MuleException {
      startIfNeeded(delegate);
    }

    @Override
    public void dispose() {
      disposeIfNeeded(delegate, LOGGER);
    }

    @Override
    public void stop() throws MuleException {
      stopIfNeeded(delegate);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return delegate.process(event);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return delegate.apply(publisher);
    }

    @Override
    public void setMessagingExceptionHandler(FlowExceptionHandler messagingExceptionHandler) {
      this.messagingExceptionHandler = messagingExceptionHandler;

    }

    @Override
    public void setComponentTracer(ComponentTracer<CoreEvent> chainComponentTracer) {
      this.chainComponentTracer = chainComponentTracer;
    }
  }

}
