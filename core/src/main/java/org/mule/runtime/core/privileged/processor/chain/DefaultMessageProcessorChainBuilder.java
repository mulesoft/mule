/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.ReferenceProcessor;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;

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
 * Constructs a chain of {@link Processor}s and wraps the invocation of the chain in a composite MessageProcessor. Both
 * MessageProcessors and InterceptingMessageProcessor's can be chained together arbitrarily in a single chain.
 * InterceptingMessageProcessors simply intercept the next MessageProcessor in the chain. When other non-intercepting
 * MessageProcessors are used an adapter is used internally to chain the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
public class DefaultMessageProcessorChainBuilder extends AbstractMessageProcessorChainBuilder {

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

    // Start from last but one message processor and work backwards
    for (int i = processors.size() - 1; i >= 0; i--) {
      Processor processor = initializeMessageProcessor(processors.get(i));
      if (processor instanceof InterceptingMessageProcessor && (!(processor instanceof ReferenceProcessor)
          || ((ReferenceProcessor) processor).getReferencedProcessor() instanceof InterceptingMessageProcessor)) {
        InterceptingMessageProcessor interceptingProcessor = (InterceptingMessageProcessor) processor;
        // Processor is intercepting so we can't simply iterate
        if (i + 1 < processors.size()) {
          // Wrap processors in chain, unless single processor that is already a chain
          final MessageProcessorChain innerChain = createSimpleChain(tempList);
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
    // Create the final chain using the current tempList after reserve iteration is complete. This temp
    // list contains the first n processors in the chain that are not intercepting.. with processor n+1
    // having been injected as the listener of processor n
    Processor head = tempList.size() == 1 ? tempList.get(0) : createSimpleChain(tempList);
    processorsForLifecycle.addFirst(head);
    return createInterceptingChain(head, processors, processorsForLifecycle);
  }

  protected MessageProcessorChain createSimpleChain(List<Processor> tempList) {
    if (tempList.size() == 1 && tempList.get(0) instanceof SimpleMessageProcessorChain) {
      return (MessageProcessorChain) tempList.get(0);
    } else {
      return new SimpleMessageProcessorChain("(inner chain) of " + name, ofNullable(processingStrategy),
                                             new ArrayList<>(tempList));
    }
  }

  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorsForLifecycle) {
    return new DefaultMessageProcessorChain("(outer intercepting chain) of " + name, ofNullable(processingStrategy), head,
                                            processors, processorsForLifecycle);
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

  static class SimpleMessageProcessorChain extends AbstractMessageProcessorChain {

    SimpleMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                List<Processor> processors) {
      super(name, processingStrategyOptional, processors);
    }

  }

  protected static class DefaultMessageProcessorChain extends AbstractMessageProcessorChain {

    private Processor head;
    private List<Processor> processorsForLifecycle;

    protected DefaultMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional, Processor head,
                                           List<Processor> processors,
                                           List<Processor> processorsForLifecycle) {
      super(name, processingStrategyOptional, processors);
      this.head = head;
      this.processorsForLifecycle = processorsForLifecycle;
    }

    @Override
    protected List<Processor> getMessageProcessorsForLifecycle() {
      return processorsForLifecycle;
    }

    @Override
    protected List<Processor> getProcessorsToExecute() {
      return singletonList(head);
    }

  }

  /**
   * Helper method to create a lazy processor from a chain builder so the chain builder can get access to a
   * {@link FlowConstruct}{@link ProcessingStrategy}.
   *
   * @param chainBuilder the chain builder
   * @param muleContext the context
   * @param processingStrategySupplier a supplier of the processing strategy.
   * @return a lazy processor that will build the chain upon the first request.
   */
  public static MessageProcessorChain newLazyProcessorChainBuilder(AbstractMessageProcessorChainBuilder chainBuilder,
                                                                   MuleContext muleContext,
                                                                   Supplier<ProcessingStrategy> processingStrategySupplier) {
    return new AbstractMessageProcessorChain(chainBuilder.name, empty(), chainBuilder.processors) {

      private MessageProcessorChain delegate;

      @Override
      public void initialise() throws InitialisationException {
        chainBuilder.setProcessingStrategy(processingStrategySupplier.get());
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
    };
  }

}
