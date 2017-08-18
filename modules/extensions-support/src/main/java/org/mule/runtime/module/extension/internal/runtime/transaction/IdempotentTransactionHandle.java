/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.extension.api.tx.TransactionHandle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Idempotent {@link TransactionHandle} implementation.
 * <p>
 * This will execute only one time a resolution transaction action.
 *
 * @param <T> The type of transactional resource to be used
 * @since 4.0
 */
public abstract class IdempotentTransactionHandle<T> implements TransactionHandle {

  private final AtomicBoolean txResolved = new AtomicBoolean(false);
  private final T txResource;
  private final CheckedConsumer<T> commitAction;
  private final CheckedConsumer<T> rollbackAction;

  /**
   * Creates a new instance of {@link IdempotentTransactionHandle}
   *
   * @param txResource     Transactional Resource to which an transactional action will applied
   * @param commitAction   {@link Consumer} to be called with the tx resource when a commit action is required to be executed
   * @param rollbackAction {@link Consumer} to be called with the tx resource when a rollback action is required to be executed
   */
  IdempotentTransactionHandle(T txResource, CheckedConsumer<T> commitAction, CheckedConsumer<T> rollbackAction) {
    this.txResource = txResource;
    this.commitAction = commitAction;
    this.rollbackAction = rollbackAction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTransacted() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws TransactionException {
    resolveTxAs(() -> {
      try {
        commitAction.accept(txResource);
      } catch (Throwable e) {
        throw new TransactionException(e);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws TransactionException {
    resolveTxAs(() -> {
      try {
        rollbackAction.accept(txResource);
      } catch (Throwable e) {
        throw new TransactionException(e);
      }
    });
  }

  private void resolveTxAs(CheckedRunnable task) {
    if (txResolved.compareAndSet(false, true)) {
      task.run();
      TransactionCoordination.getInstance().clear();
    }
  }
}
