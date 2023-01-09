/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.HasLocation;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.mule.runtime.core.privileged.profiling.tracing.InitialSpanInfoAware;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Constructs a chain of {@link Processor}s and wraps the invocation of the chain in a composite MessageProcessor. Both
 * MessageProcessors and InterceptingMessageProcessor's can be chained together arbitrarily in a single chain.
 * InterceptingMessageProcessors simply intercept the next MessageProcessor in the chain. When other non-intercepting
 * MessageProcessors are used an adapter is used internally to chain the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
@NoExtend
public class DefaultMessageProcessorChainBuilder extends AbstractMessageProcessorChainBuilder
    implements InitialSpanInfoAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageProcessorChainBuilder.class);

  /**
   * This builder supports the chaining together of message processors that intercept and also those that don't. While one can
   * iterate over message processor intercepting message processors need to be chained together. One solution is make all message
   * processors intercepting (via adaption) and chain them all together, this results in huge stack traces and recursive calls
   * with adaptor. The alternative is to build the chain in such a way that we iterate when we can and chain where we need to.
   * <br>
   * We iterate over the list of message processor to be chained together in reverse order collecting up those that can be
   * iterated over in a temporary list, as soon as we have an intercepting message processor we create a
   * DefaultMessageProcessorChain using the temporary list and set it as a listener of the intercepting message processor and then
   * we continue with the algorithm
   */
  @Override
  public MessageProcessorChain build() {
    LinkedList<Processor> tempList = new LinkedList<>();

    final LinkedList<Processor> processorsForLifecycle = new LinkedList<>();
    boolean atLeastOneIntercepting = false;

    // Start from last but one message processor and work backwards
    for (int i = processors.size() - 1; i >= 0; i--) {
      Processor processor = initializeMessageProcessor(processors.get(i));
      if (processor instanceof InterceptingMessageProcessor) {
        atLeastOneIntercepting = true;
        InterceptingMessageProcessor interceptingProcessor = (InterceptingMessageProcessor) processor;
        // Processor is intercepting so we can't simply iterate
        if (i + 1 < processors.size()) {
          // Wrap processors in chain, unless single processor that is already a chain
          final MessageProcessorChain innerChain =
              createSimpleInterceptedChain(tempList,
                                           interceptingProcessor.isBlocking() ? of(BLOCKING_PROCESSING_STRATEGY_INSTANCE)
                                               : ofNullable(processingStrategy));
          processorsForLifecycle.addFirst(innerChain);
          interceptingProcessor.setListener(innerChain);
        }
        tempList = new LinkedList<>(singletonList(processor));
      } else {
        // Processor is not intercepting so we can invoke it using iteration
        // (add to temp list)
        tempList.addFirst(processor);
      }
    }

    if (atLeastOneIntercepting) {
      // Create the final chain using the current tempList after reverse iteration is complete. This temp
      // list contains the first n processors in the chain that are not intercepting.. with processor n+1
      // having been injected as the listener of processor n
      MessageProcessorChain head = createSimpleChain(tempList, ofNullable(processingStrategy));
      processorsForLifecycle.addFirst(head);
      return createInterceptingChain(head, processors, processorsForLifecycle);
    } else {
      return createSimpleChain(tempList, ofNullable(processingStrategy));
    }
  }

  protected MessageProcessorChain createSimpleChain(List<Processor> tempList,
                                                    Optional<ProcessingStrategy> processingStrategyOptional) {
    if (tempList.size() == 1 && tempList.get(0) instanceof DefaultMessageProcessorChain) {
      DefaultMessageProcessorChain messageProcessorChain = (DefaultMessageProcessorChain) tempList.get(0);
      if (chainInitialSpanInfo != null) {
        messageProcessorChain.setInitialSpanInfo(chainInitialSpanInfo);
      }
      return messageProcessorChain;
    } else {
      DefaultMessageProcessorChain messageProcessorChain =
          new DefaultMessageProcessorChain(name != null ? "(chain) of " + name : "(chain)",
                                           processingStrategyOptional,
                                           new ArrayList<>(tempList),
                                           messagingExceptionHandler,
                                           location);
      if (chainInitialSpanInfo != null) {
        messageProcessorChain.setInitialSpanInfo(chainInitialSpanInfo);
      }
      return messageProcessorChain;
    }
  }

  private MessageProcessorChain createSimpleInterceptedChain(List<Processor> tempList,
                                                             Optional<ProcessingStrategy> processingStrategyOptional) {
    if (tempList.size() == 1 && tempList.get(0) instanceof DefaultMessageProcessorChain) {
      DefaultMessageProcessorChain messageProcessorChain = (DefaultMessageProcessorChain) tempList.get(0);
      if (chainInitialSpanInfo != null) {
        messageProcessorChain.setInitialSpanInfo(chainInitialSpanInfo);
      }
      return messageProcessorChain;
    } else {
      DefaultMessageProcessorChain messageProcessorChain =
          new DefaultMessageProcessorChain(name != null ? "(chain) of " + name : "(chain)",
                                           processingStrategyOptional,
                                           new ArrayList<>(tempList),
                                           NullExceptionHandler.getInstance(),
                                           location);
      if (chainInitialSpanInfo != null) {
        messageProcessorChain.setInitialSpanInfo(chainInitialSpanInfo);
      }
      return messageProcessorChain;
    }
  }

  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorsForLifecycle) {
    InterceptingMessageProcessorChain messageProcessorChain =
        new InterceptingMessageProcessorChain(name != null ? "(intercepting chain) of " + name : "(intercepting chain)",
                                              ofNullable(processingStrategy), head,
                                              processors, processorsForLifecycle, NullExceptionHandler.getInstance(),
                                              location);
    if (chainInitialSpanInfo != null) {
      messageProcessorChain.setInitialSpanInfo(chainInitialSpanInfo);
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
  public DefaultMessageProcessorChainBuilder chain(MessageProcessorBuilder... builders) {
    for (MessageProcessorBuilder messageProcessorBuilder : builders) {
      this.processors.add(messageProcessorBuilder);
    }
    return this;
  }

  public DefaultMessageProcessorChainBuilder chainBefore(Processor processor) {
    this.processors.add(0, processor);
    return this;
  }

  public DefaultMessageProcessorChainBuilder chainBefore(MessageProcessorBuilder builder) {
    this.processors.add(0, builder);
    return this;
  }

  @Override
  public void setInitialSpanInfo(InitialSpanInfo initialSpanInfo) {
    this.chainInitialSpanInfo = initialSpanInfo;
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


  static class InterceptingMessageProcessorChain extends AbstractMessageProcessorChain implements HasLocation {

    private final Processor head;
    private final List<Processor> processorsForLifecycle;
    private final ComponentLocation pipeLineLocation;

    protected InterceptingMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                                Processor head,
                                                List<Processor> processors,
                                                List<Processor> processorsForLifecycle,
                                                FlowExceptionHandler messagingExceptionHandler) {
      this(name, processingStrategyOptional, head, processors, processorsForLifecycle, messagingExceptionHandler, null);
    }

    protected InterceptingMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                                Processor head,
                                                List<Processor> processors,
                                                List<Processor> processorsForLifecycle,
                                                FlowExceptionHandler messagingExceptionHandler,
                                                ComponentLocation pipeLineLocation) {
      super(name, processingStrategyOptional, processors, messagingExceptionHandler);
      this.head = head;
      this.processorsForLifecycle = processorsForLifecycle;
      this.pipeLineLocation = pipeLineLocation;
    }

    @Override
    protected List<Processor> getMessageProcessorsForLifecycle() {
      return processorsForLifecycle;
    }

    @Override
    protected List<Processor> getProcessorsToExecute() {
      return singletonList(head);
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
      implements MessagingExceptionHandlerAware, InitialSpanInfoAware {

    private final AbstractMessageProcessorChainBuilder chainBuilder;
    private final Supplier<ProcessingStrategy> processingStrategySupplier;
    private FlowExceptionHandler messagingExceptionHandler;
    private MessageProcessorChain delegate;
    private InitialSpanInfo chainInitialSpanInfo;

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
      chainBuilder.setChainInitialSpanInfo(chainInitialSpanInfo);
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
    public void setInitialSpanInfo(InitialSpanInfo chainInitialSpanInfo) {
      this.chainInitialSpanInfo = chainInitialSpanInfo;
    }
  }

}
