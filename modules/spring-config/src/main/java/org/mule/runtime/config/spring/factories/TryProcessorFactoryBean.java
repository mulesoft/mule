/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.lang.String.format;
import static java.util.ServiceLoader.load;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transaction.TypedTransactionFactory;
import org.mule.runtime.core.processor.TryMessageProcessor;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionType;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

/**
 * Generates an object that wraps the invocation of the next {@link org.mule.runtime.core.api.processor.Processor} with a
 * transaction. If the {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the
 * next {@code org.mule.runtime.core.api.processor.MessageProcessor} is invoked directly.
 *
 * @since 4.0
 *
 * TODO MULE-12726 Remove TryProcessorFactoryBean
 */
public class TryProcessorFactoryBean extends AbstractAnnotatedObject implements FactoryBean {

  protected List messageProcessors;
  protected MessagingExceptionHandler exceptionListener;
  protected String transactionalAction;
  private TransactionType transactionType;

  @Override
  public Class getObjectType() {
    return MessageProcessorChain.class;
  }

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public Object getObject() throws Exception {
    TryMessageProcessor txProcessor = new TryMessageProcessor();
    txProcessor.setAnnotations(getAnnotations());
    txProcessor.setExceptionListener(this.exceptionListener);
    txProcessor.setTransactionConfig(createTransactionConfig(this.transactionalAction, this.transactionType));
    txProcessor.setMessageProcessors(messageProcessors);
    return txProcessor;
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

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setExceptionListener(MessagingExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  public void setTransactionalAction(String action) {
    this.transactionalAction = action;
  }

  public void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }
}
