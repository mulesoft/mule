/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.errorInvokingMessageProcessorWithinTransaction;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createScopeTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_INDIFFERENT;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wraps the invocation of a list of nested processors {@link org.mule.runtime.core.api.processor.Processor} with a transaction.
 * If the {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@link org.mule.runtime.core.api.processor.Processor} is invoked directly.
 */
public class TryScope extends AbstractMessageProcessorOwner implements Scope {

  private static final Logger LOGGER = LoggerFactory.getLogger(TryScope.class);

  protected MessageProcessorChain nestedChain;
  protected MuleTransactionConfig transactionConfig;
  private FlowExceptionHandler messagingExceptionHandler;
  private List<Processor> processors;

  @Override
  public CoreEvent process(final CoreEvent event) throws MuleException {
    if (nestedChain == null) {
      return event;
    } else {
      ExecutionTemplate<CoreEvent> executionTemplate =
          createScopeTransactionalExecutionTemplate(muleContext, transactionConfig);
      ExecutionCallback<CoreEvent> processingCallback = () -> {
        try {
          CoreEvent e = processToApply(event, p -> from(p)
              .flatMap(request -> processWithChildContext(request, nestedChain, ofNullable(getLocation()),
                                                          messagingExceptionHandler)));
          return e;
        } catch (Exception e) {
          throw e;
        }
      };

      try {
        return executionTemplate.execute(processingCallback);
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(errorInvokingMessageProcessorWithinTransaction(nestedChain, transactionConfig), e);
      }
    }
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    if (nestedChain == null) {
      return publisher;
    } else if (isTransactionActive() || transactionConfig.getAction() != ACTION_INDIFFERENT) {
      return Scope.super.apply(publisher);
    } else {
      return from(publisher)
          .flatMap(event -> processWithChildContext(event, nestedChain, ofNullable(getLocation()), messagingExceptionHandler));
    }
  }

  /**
   * Configures the {@link FlowExceptionHandler} that should be used to handle any errors that occur in this scope.
   *
   * @param exceptionListener the {@link FlowExceptionHandler} to be used.
   */
  public void setExceptionListener(FlowExceptionHandler exceptionListener) {
    this.messagingExceptionHandler = exceptionListener;
  }

  /**
   * Configures the {@link TransactionConfig} that that defines the transactional behaviour of this scope.
   *
   * @param transactionConfig the {@link TransactionConfig} to be used.
   */
  public void setTransactionConfig(MuleTransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
  }

  /**
   * Obtain the {@link TransactionConfig} configured that defines transactional behaviour of this scope.
   *
   * @return the configured {@link TransactionConfig}.
   */
  public MuleTransactionConfig getTransactionConfig() {
    return transactionConfig;
  }

  /**
   * Configure the nested {@link Processor}'s that error handling and transactional behaviour should be applied to.
   *
   * @param processors
   */
  public void setMessageProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.nestedChain = newChain(getProcessingStrategy(locator, getRootContainerLocation()), processors);
    if (messagingExceptionHandler == null) {
      messagingExceptionHandler = muleContext.getDefaultErrorHandler(of(getRootContainerLocation().toString()));
    }
    initialiseIfNeeded(messagingExceptionHandler, true, muleContext);
    super.initialise();
  }

  @Override
  public void dispose() {
    disposeIfNeeded(messagingExceptionHandler, LOGGER);
    super.dispose();
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(messagingExceptionHandler);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(messagingExceptionHandler);
    super.stop();
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(nestedChain);
  }

}
