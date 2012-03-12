/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.process.ProcessingCallback;
import org.mule.process.ProcessingTemplate;
import org.mule.process.TransactionalErrorHandlingProcessingTemplate;
import org.mule.transaction.MuleTransactionConfig;

/**
 * Wraps the invocation of the next {@link org.mule.api.processor.MessageProcessor} with a transaction. If
 * the {@link org.mule.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@link org.mule.api.processor.MessageProcessor} is invoked directly.
 */
public class TransactionalInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor implements Initialisable
{
    protected MessagingExceptionHandler exceptionListener;
    protected MuleTransactionConfig transactionConfig;

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else
        {
            ProcessingTemplate<MuleEvent> processingTemplate = TransactionalErrorHandlingProcessingTemplate.createMainProcessingTemplate(muleContext, transactionConfig, exceptionListener);
            ProcessingCallback<MuleEvent> processingCallback = new ProcessingCallback<MuleEvent>()
            {
                public MuleEvent process() throws Exception
                {
                    return processNext(event);
                }
            };

            try
            {
                return processingTemplate.execute(processingCallback);
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

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (this.exceptionListener == null)
        {
            this.exceptionListener = muleContext.getDefaultExceptionStrategy();
        }
    }

    public void setTransactionConfig(MuleTransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }
}
