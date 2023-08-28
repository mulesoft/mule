/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transaction.TransactionFactory;

import java.util.Map;

/**
 *
 */
public final class SingleResourceTransactionFactoryManager {

  private Map<Class, TransactionFactory> transactionFactories = new SmallMap<>();
  private Map<Class, TransactionFactory> transactionFactoriesCache = new SmallMap<>();

  public void registerTransactionFactory(Class supportedType, TransactionFactory transactionFactory) {
    this.transactionFactories.put(supportedType, transactionFactory);
  }

  public boolean supports(Class type) {
    return this.transactionFactories.containsKey(type);
  }

  public TransactionFactory getTransactionFactoryFor(Class type) {
    TransactionFactory transactionFactory = transactionFactoriesCache.get(type);
    if (transactionFactory == null) {
      for (Class transactionResourceType : transactionFactories.keySet()) {
        if (transactionResourceType.isAssignableFrom(type)) {
          transactionFactory = transactionFactories.get(transactionResourceType);
          this.transactionFactoriesCache.put(type, transactionFactory);
          break;
        }
      }
    }
    if (transactionFactory == null) {
      throw new MuleRuntimeException(CoreMessages.createStaticMessage(String
          .format("No %s for transactional resource %s", TransactionFactory.class.getName(), type.getName())));
    }
    return transactionFactory;
  }
}
