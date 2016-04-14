/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.TransactionalExecutionTemplate;

/**
 * Wraps the invocation of the next {@link MessageProcessor} with a transaction. If
 * the {@link TransactionConfig} is null then no transaction is used and the next
 * {@link MessageProcessor} is invoked directly.
 */
public class EndpointTransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
{
    protected TransactionConfig transactionConfig;

    public EndpointTransactionalInterceptingMessageProcessor(TransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else
        {
            ExecutionTemplate<MuleEvent> executionTemplate = TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, transactionConfig);
            ExecutionCallback<MuleEvent> processingCallback = new ExecutionCallback<MuleEvent>()
            {
                public MuleEvent process() throws Exception
                {
                    return processNext(event);
                }
            };

            try
            {
                return executionTemplate.execute(processingCallback);
            }
            catch (MuleException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(CoreMessages.errorInvokingMessageProcessorWithinTransaction(
                    next, transactionConfig), e);
            }
        }
    }
}
