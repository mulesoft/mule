/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessors;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionFactory;
import org.mule.processor.DelegateTransactionFactory;
import org.mule.processor.EndpointTransactionalInterceptingMessageProcessor;
import org.mule.processor.TransactionalInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import org.mule.transaction.MuleTransactionConfig;
import org.springframework.beans.factory.FactoryBean;

public class TransactionalMessageProcessorsFactoryBean implements FactoryBean
{

    protected List messageProcessors;
    protected MessagingExceptionHandler exceptionListener;
    protected String action;

    public Class getObjectType()
    {
        return TransactionalInterceptingMessageProcessor.class;
    }

    public void setMessageProcessors(List messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public Object getObject() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("'transaction' child processor chain");
        TransactionalInterceptingMessageProcessor txProcessor =
            new TransactionalInterceptingMessageProcessor();
        txProcessor.setExceptionListener(this.exceptionListener);
        MuleTransactionConfig transactionConfig = createTransactionConfig(this.action);
        txProcessor.setTransactionConfig(transactionConfig);
        transactionConfig.setFactory(getTransactionFactory());
        builder.chain(txProcessor);
        for (Object processor : messageProcessors)
        {
            if (processor instanceof MessageProcessor)
            {
                builder.chain((MessageProcessor) processor);
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                builder.chain((MessageProcessorBuilder) processor);
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }
        return MessageProcessors.lifecyleAwareMessageProcessorWrapper(builder.build());
    }

    protected TransactionFactory getTransactionFactory()
    {
        return new DelegateTransactionFactory();
    }

    protected MuleTransactionConfig createTransactionConfig(String action)
    {
        MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
        transactionConfig.setActionAsString(action);
        return transactionConfig;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public void setAction(String action)
    {
        this.action = action;
    }
}
