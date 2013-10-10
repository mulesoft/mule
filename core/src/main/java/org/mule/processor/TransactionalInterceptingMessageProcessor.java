/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionTemplate;

/**
 * Wraps the invocation of the next {@link MessageProcessor} with a transaction. If
 * the {@link TransactionConfig} is null then no transaction is used and the next
 * {@link MessageProcessor} is invoked directly.
 */
public class TransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
{
    protected TransactionConfig transactionConfig;

    public TransactionalInterceptingMessageProcessor(TransactionConfig transactionConfig)
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
            TransactionTemplate<MuleEvent> tt = new TransactionTemplate<MuleEvent>(transactionConfig, event.getMuleContext());
            TransactionCallback<MuleEvent> cb = new TransactionCallback<MuleEvent>()
            {
                public MuleEvent doInTransaction() throws Exception
                {
                    return processNext(event);
                }
            };

            try
            {
                return tt.execute(cb);
            }
            catch (MessagingException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new MessagingException(CoreMessages.errorInvokingMessageProcessorWithinTransaction(
                    next, transactionConfig), event, e);
            }
        }
    }
}
