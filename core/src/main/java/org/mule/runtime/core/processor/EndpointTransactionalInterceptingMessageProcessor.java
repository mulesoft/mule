/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;

/**
 * Wraps the invocation of the next {@link MessageProcessor} with a transaction. If the {@link TransactionConfig} is null then no
 * transaction is used and the next {@link MessageProcessor} is invoked directly.
 */
public class EndpointTransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements NonBlockingSupported {

  protected TransactionConfig transactionConfig;

  public EndpointTransactionalInterceptingMessageProcessor(TransactionConfig transactionConfig) {
    this.transactionConfig = transactionConfig;
  }

  @Override
  public MuleEvent process(final MuleEvent event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      ExecutionTemplate<MuleEvent> executionTemplate =
          TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, transactionConfig);
      ExecutionCallback<MuleEvent> processingCallback = new ExecutionCallback<MuleEvent>() {

        @Override
        public MuleEvent process() throws Exception {
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
