/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.errorInvokingMessageProcessorWithinTransaction;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createScopeTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_BEGIN_OR_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_INDIFFERENT;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.transaction.TransactionUtils.profileTransactionAction;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.popTxFromSubscriberContext;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.pushTxToSubscriberContext;
import static org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler.reuseGlobalErrorHandler;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

import java.util.List;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.util.context.Context;

/**
 * Wraps the invocation of a list of nested processors {@link org.mule.runtime.core.api.processor.Processor} with a transaction.
 * If the {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@link org.mule.runtime.core.api.processor.Processor} is invoked directly.
 */
public class TryScope extends AbstractMessageProcessorOwner implements Scope {

  private static final Logger LOGGER = getLogger(TryScope.class);

  protected MessageProcessorChain nestedChain;
  protected MuleTransactionConfig transactionConfig;
  private FlowExceptionHandler messagingExceptionHandler;
  private List<Processor> processors;

  @Inject
  private ProfilingService profilingService;

  private ProfilingDataProducer<TransactionProfilingEventContext, Object> continueProducer;
  private ProfilingDataProducer<TransactionProfilingEventContext, Object> startProducer;
  private ProfilingDataProducer<TransactionProfilingEventContext, Object> commitProducer;


  @Override
  public CoreEvent process(final CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    if (transactionConfig.getAction() == ACTION_INDIFFERENT) {
      return from(publisher).doOnNext(e -> profileTransactionAction(continueProducer, TX_CONTINUE, getLocation()))
          .transform(nestedChain);
    }

    ExecutionTemplate<CoreEvent> executionTemplate = createScopeTransactionalExecutionTemplate(muleContext, transactionConfig);
    final I18nMessage txErrorMessage = errorInvokingMessageProcessorWithinTransaction(nestedChain, transactionConfig);

    return subscriberContext()
        .flatMapMany(ctx -> from(publisher)
            .handle((event, sink) -> {
              final boolean txPrevoiuslyActive = isTransactionActive();
              Transaction previousTx = getCurrentTx();
              try {
                sink.next(executionTemplate.execute(() -> {
                  handlePreviousTransaction(txPrevoiuslyActive, previousTx, getCurrentTx());
                  profileBeforeExecution(txPrevoiuslyActive);
                  CoreEvent result = processBlocking(ctx, event);
                  profileAfterExecution(txPrevoiuslyActive);
                  return result;
                }));
              } catch (Exception e) {
                final Throwable unwrapped = unwrap(e);

                if (unwrapped instanceof MuleException) {
                  sink.error(unwrapped);
                } else {
                  sink.error(new DefaultMuleException(txErrorMessage, unwrapped));
                }
              }
            }));
  }

  private void profileBeforeExecution(boolean txPrevoiuslyActive) {
    if (txPrevoiuslyActive) {
      profileTransactionAction(continueProducer, TX_CONTINUE, getLocation());
    } else {
      profileTransactionAction(startProducer, TX_START, getLocation());
    }
  }

  private void profileAfterExecution(boolean txPrevoiuslyActive) {
    if (txPrevoiuslyActive) {
      profileTransactionAction(continueProducer, TX_CONTINUE, getLocation());
    } else {
      profileTransactionAction(commitProducer, TX_COMMIT, getLocation());
    }
  }

  private void handlePreviousTransaction(final boolean txPrevoiuslyActive, Transaction previousTx, Transaction currentTx) {
    // Whether there wasn't a tx and now there is, or if there is a newer one (if we have a nested tx, using xa)
    // we must set the component location of this try scope
    if ((!txPrevoiuslyActive && isTransactionActive()) || (txPrevoiuslyActive && previousTx != currentTx)) {
      TransactionAdapter transaction = (TransactionAdapter) currentTx;
      transaction.setComponentLocation(getLocation());
    }
  }

  private CoreEvent processBlocking(Context ctx, CoreEvent event) throws MuleException {
    try {
      return just(event)
          .subscriberContext(popTxFromSubscriberContext())
          .transform(nestedChain)
          .onErrorStop()
          // This is needed for all cases because of the way that transactional try cache invokes its inner chain
          .subscriberContext(innerCtx -> innerCtx.put(WITHIN_PROCESS_TO_APPLY, true))
          .subscriberContext(pushTxToSubscriberContext(getLocation().getLocation()))
          .subscriberContext(ctx)
          .block();
    } catch (Throwable e) {
      if (e.getCause() instanceof InterruptedException) {
        currentThread().interrupt();
      }
      throw rxExceptionToMuleException(e);
    }
  }

  private Transaction getCurrentTx() {
    return TransactionCoordination.getInstance().getTransaction();
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
    if (messagingExceptionHandler == null) {
      messagingExceptionHandler = muleContext.getDefaultErrorHandler(of(getRootContainerLocation().toString()));
      if (shouldSetLocation()) {
        ((ErrorHandler) messagingExceptionHandler)
            .setExceptionListenersLocation(this.getLocation());
      }
    }
    this.nestedChain = buildNewChainWithListOfProcessors(getProcessingStrategy(locator, this), processors,
                                                         messagingExceptionHandler, getLocation().getLocation());
    initialiseIfNeeded(messagingExceptionHandler, true, muleContext);
    continueProducer = profilingService.getProfilingDataProducer(TX_CONTINUE);
    startProducer = profilingService.getProfilingDataProducer(TX_START);
    commitProducer = profilingService.getProfilingDataProducer(TX_COMMIT);
    super.initialise();
  }

  private boolean shouldSetLocation() {
    return (!reuseGlobalErrorHandler() || !(messagingExceptionHandler instanceof GlobalErrorHandler))
        && messagingExceptionHandler instanceof ErrorHandler;
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


  @Override
  public ProcessingType getProcessingType() {
    byte txAction = transactionConfig.getAction();
    if (txAction == ACTION_ALWAYS_BEGIN || txAction == ACTION_BEGIN_OR_JOIN) {
      return ProcessingType.BLOCKING;
    } else {
      return ProcessingType.CPU_LITE;
    }
  }
}
