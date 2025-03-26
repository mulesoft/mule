/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Optional.ofNullable;
import static java.util.ServiceLoader.load;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.inject.Inject;

/**
 * Locator which given a {@link TransactionType} will locates through SPI a {@link TransactionFactory} able to handle that kind of
 * of transaction.
 *
 * @since 4.0
 */
public final class TransactionFactoryLocator implements Disposable {

  @Inject
  private MuleContext muleContext;

  private final Map<TransactionType, TransactionFactory> factories = new SmallMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private boolean initialized = false;

  /**
   * Given a {@link TransactionType} will look through SPI a {@link TransactionFactory} able to handle that kind of of
   * transaction.
   *
   * @param type The {@link TransactionType} that the {@link TransactionFactory} should handle.
   * @return An {@link Optional} {@link TransactionFactory}
   */
  public Optional<TransactionFactory> lookUpTransactionFactory(TransactionType type) {
    if (!initialized) {
      Lock writeLock = lock.writeLock();
      try {
        writeLock.lock();
        if (!initialized) {
          try {
            factories.putAll(getAvailableFactories());
          } catch (InitialisationException e) {
            throw new MuleRuntimeException(e);
          }
          initialized = true;
        }
      } finally {
        writeLock.unlock();
      }
    }
    Lock readLock = lock.readLock();
    TransactionFactory value;
    try {
      readLock.lock();
      value = factories.get(type);
    } finally {
      readLock.unlock();
    }
    return ofNullable(value);
  }

  @Override
  public void dispose() {
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      factories.clear();
      initialized = false;
    } finally {
      writeLock.unlock();
    }
  }

  private Map<TransactionType, TypedTransactionFactory> getAvailableFactories() throws InitialisationException {
    Map<TransactionType, TypedTransactionFactory> discoveredFactories = new EnumMap<>(TransactionType.class);
    final var serviceLoader = load(TypedTransactionFactory.class, this.getClass().getClassLoader());
    for (TypedTransactionFactory factory : serviceLoader) {
      initialiseIfNeeded(factory, true, muleContext);
      discoveredFactories.put(factory.getType(), factory);
    }
    return discoveredFactories;
  }
}
