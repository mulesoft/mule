/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.execution.TransactionalExecutionTemplate;

/**
 * Wraps the invocation of the next {@link Processor} with a transaction. If the {@link TransactionConfig} is null then no
 * transaction is used and the next {@link Processor} is invoked directly.
 */
public class EndpointTransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor {

  protected TransactionConfig transactionConfig;

  public EndpointTransactionalInterceptingMessageProcessor(TransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
  }

  @Override
  public Event process(final Event event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      ExecutionTemplate<Event> executionTemplate =
          TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, transactionConfig);
      ExecutionCallback<Event> processingCallback = new ExecutionCallback<Event>() {

        @Override
        public Event process() throws Exception {
          return processNext(event);
        }
      };

      try {
        return executionTemplate.execute(processingCallback);
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.errorInvokingMessageProcessorWithinTransaction(next, transactionConfig), e);
      }
    }
  }
}
