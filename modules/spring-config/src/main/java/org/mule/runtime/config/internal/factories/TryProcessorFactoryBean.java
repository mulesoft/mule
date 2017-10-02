/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.processor.TryScope;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.springframework.beans.factory.FactoryBean;

import javax.inject.Inject;
import java.util.List;

/**
 * Generates an object that wraps the invocation of the next {@link org.mule.runtime.core.api.processor.Processor} with a
 * transaction. If the {@link org.mule.runtime.core.api.transaction.TransactionConfig} is null then no transaction is used and the
 * next {@code org.mule.runtime.core.api.processor.MessageProcessor} is invoked directly.
 *
 * @since 4.0
 *
 *        TODO MULE-12726 Remove TryProcessorFactoryBean
 */
public class TryProcessorFactoryBean extends AbstractComponent implements FactoryBean {

  protected List messageProcessors;
  protected FlowExceptionHandler exceptionListener;
  protected String transactionalAction;
  private TransactionType transactionType;

  @Inject
  private TransactionFactoryLocator transactionFactoryLocator;

  @Override
  public Class getObjectType() {
    return MessageProcessorChain.class;
  }

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public Object getObject() throws Exception {
    TryScope txProcessor = new TryScope();
    txProcessor.setAnnotations(getAnnotations());
    txProcessor.setExceptionListener(this.exceptionListener);
    txProcessor.setTransactionConfig(createTransactionConfig(this.transactionalAction, this.transactionType));
    txProcessor.setMessageProcessors(messageProcessors == null ? emptyList() : messageProcessors);
    return txProcessor;
  }

  protected MuleTransactionConfig createTransactionConfig(String action, TransactionType type) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    transactionConfig.setFactory(transactionFactoryLocator.lookUpTransactionFactory(type)
        .orElseThrow(() -> new IllegalArgumentException(format("Unable to create Try Scope with a Transaction Type: [%s]. No factory available for this transaction type",
                                                               type))));
    return transactionConfig;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setExceptionListener(FlowExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  public void setTransactionalAction(String action) {
    this.transactionalAction = action;
  }

  public void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }
}
