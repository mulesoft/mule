/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.processor.DelegateTransactionFactory;
import org.mule.runtime.core.processor.TransactionalInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import java.util.List;

/**
 * Wraps the invocation of the next {@link org.mule.runtime.core.api.processor.MessageProcessor} with a transaction. If the
 * {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@code org.mule.runtime.core.api.processor.MessageProcessor} is invoked directly.
 *
 * @since 4.0
 */
public class TransactionalMessageProcessor extends TransactionalInterceptingMessageProcessor
    implements Lifecycle, MuleContextAware {

  protected List messageProcessors;
  protected String action;
  private MessageProcessorChain delegate;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    return delegate.process(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.setName("'transaction' child processor chain");
    TransactionalInterceptingMessageProcessor txProcessor = new TransactionalInterceptingMessageProcessor();
    txProcessor.setExceptionListener(this.exceptionListener);
    MuleTransactionConfig transactionConfig = createTransactionConfig(this.action);
    txProcessor.setTransactionConfig(transactionConfig);
    transactionConfig.setFactory(getTransactionFactory());
    builder.chain(txProcessor);
    for (Object processor : messageProcessors) {
      if (processor instanceof MessageProcessor) {
        builder.chain((MessageProcessor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
      if (processor instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(exceptionListener);
      }
    }
    try {
      delegate = builder.build();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
    super.initialise();
  }

  protected TransactionFactory getTransactionFactory() {
    return new DelegateTransactionFactory();
  }

  protected MuleTransactionConfig createTransactionConfig(String action) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    return transactionConfig;
  }

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  public void setAction(String action) {
    this.action = action;
  }
}
