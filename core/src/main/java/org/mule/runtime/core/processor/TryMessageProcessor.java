/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.lang.String.format;
import static java.util.ServiceLoader.load;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transaction.TypedTransactionFactory;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transaction.TransactionType;

import java.util.Iterator;
import java.util.List;

/**
 * Wraps the invocation of the next {@link org.mule.runtime.core.api.processor.Processor} with a transaction. If the
 * {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the next
 * {@code org.mule.runtime.core.api.processor.MessageProcessor} is invoked directly.
 *
 * @since 4.0
 */
public class TryMessageProcessor extends TransactionalInterceptingMessageProcessor
    implements Lifecycle, MuleContextAware {

  protected List messageProcessors;
  protected String transactionalAction;
  private TransactionType transactionType;
  private MessageProcessorChain delegate;

  @Override
  public Event process(Event event) throws MuleException {
    return delegate.process(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.setName("'transaction' child processor chain");
    TransactionalInterceptingMessageProcessor txProcessor = new TransactionalInterceptingMessageProcessor();
    txProcessor.setExceptionListener(this.exceptionListener);
    txProcessor.setTransactionConfig(createTransactionConfig(this.transactionalAction, this.transactionType));
    transactionConfig.setFactory(getTransactionFactory());
    builder.chain(txProcessor);
    for (Object processor : messageProcessors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
      if (processor instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(exceptionListener);
      }
    }
    delegate = builder.build();
  }

  protected TransactionFactory getTransactionFactory() {
    return new DelegateTransactionFactory();
  }

  protected MuleTransactionConfig createTransactionConfig(String action, TransactionType type) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    transactionConfig.setFactory(lookUpTransactionFactory(type));
    return transactionConfig;
  }

  private TransactionFactory lookUpTransactionFactory(TransactionType type) {
    Iterator<TypedTransactionFactory> factories = load(TypedTransactionFactory.class).iterator();
    while (factories.hasNext()) {
      TypedTransactionFactory possibleFactory = factories.next();
      if (type.equals(possibleFactory.getType())) {
        try {
          return possibleFactory.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Unable to generate a factory for transaction %s.", type)));
        }
      }
    }
    throw new IllegalArgumentException(String.format("No factory available for transaction type %s", type));
  }

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  public void setTransactionalAction(String action) {
    this.transactionalAction = action;
  }

  public void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }
}
