/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import static java.util.Optional.ofNullable;
import static java.util.ServiceLoader.load;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transaction.TypedTransactionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locator which given a {@link TransactionType} will locates through SPI a {@link TransactionFactory} able to handle
 * that kind of of transaction.
 *
 * @since 4.0
 */
public final class TransactionFactoryLocator implements Disposable {

  private Map<TransactionType, TransactionFactory> factories = new ConcurrentHashMap<>();
  private boolean initialized = false;

  /**
   * Given a {@link TransactionType} will look through SPI a {@link TransactionFactory} able to handle that kind of
   * of transaction.
   *
   * @param type The {@link TransactionType} that the {@link TransactionFactory} should handle.
   * @return An {@link Optional} {@link TransactionFactory}
   */
  public Optional<TransactionFactory> lookUpTransactionFactory(TransactionType type) {
    if (!initialized) {
      factories.putAll(getAvailableFactories());
      initialized = true;
    }
    return ofNullable(factories.computeIfAbsent(type, this::getTransactionFactory));
  }

  @Override
  public void dispose() {
    factories.clear();
  }

  private TransactionFactory getTransactionFactory(TransactionType transactionType) {
    return getAvailableFactories().get(transactionType);
  }

  private Map<TransactionType, TypedTransactionFactory> getAvailableFactories() {
    Map<TransactionType, TypedTransactionFactory> factories = new HashMap<>();
    load(TypedTransactionFactory.class).forEach(factory -> factories.put(factory.getType(), factory));
    return factories;
  }
}
