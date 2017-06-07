/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setFlowConstructIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.config.i18n.CoreMessages.errorInvokingMessageProcessorWithinTransaction;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createScopeExecutionTemplate;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

/**
 * Wraps the invocation of the next {@link org.mule.runtime.core.api.processor.Processor} with a transaction. If the
 * {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@link org.mule.runtime.core.api.processor.Processor} is invoked directly.
 */
public class TransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor implements Lifecycle {

  protected MessagingExceptionHandler exceptionListener;
  protected MuleTransactionConfig transactionConfig;

  @Override
  public Event process(final Event event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      ExecutionTemplate<Event> executionTemplate =
          createScopeExecutionTemplate(muleContext, flowConstruct, transactionConfig, exceptionListener);
      ExecutionCallback<Event> processingCallback = () -> processNext(event);

      try {
        return executionTemplate.execute(processingCallback);
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(errorInvokingMessageProcessorWithinTransaction(next, transactionConfig), e);
      }
    }
  }

  public void setExceptionListener(MessagingExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  public void setTransactionConfig(MuleTransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
  }

  public TransactionConfig getTransactionConfig() {
    return this.transactionConfig;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (exceptionListener == null) {
      exceptionListener = muleContext.getDefaultErrorHandler();
    }
    setFlowConstructIfNeeded(exceptionListener, flowConstruct);
    initialiseIfNeeded(exceptionListener, true, muleContext);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(exceptionListener, logger);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(exceptionListener);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(exceptionListener);
  }
}
