/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.util.queue.Queue;

import java.io.Serializable;

import org.slf4j.Logger;

/**
 * Queue implementation that executes operations: - If there is no transaction context then executes the operation directly to the
 * queue. - If there is a transaction context then executes the operation through the transaction context. - During queue dispose
 * a {@link QueueStoreCacheListener} will be notified
 */
public class TransactionAwareQueueStore implements Queue {

  private static final Logger LOGGER = getLogger(TransactionAwareQueueStore.class);

  private final LifecycleState deploymentLifecycleState;
  private final TransactionContextProvider transactionContextProvider;
  private final QueueStore queue;

  public TransactionAwareQueueStore(QueueStore queue, TransactionContextProvider transactionContextProvider,
                                    LifecycleState deploymentLifecycleState) {
    this.queue = queue;
    this.transactionContextProvider = transactionContextProvider;
    this.deploymentLifecycleState = deploymentLifecycleState;
  }

  @Override
  public void put(Serializable item) throws InterruptedException {
    offer(item, Long.MAX_VALUE);
  }

  @Override
  public void clear() throws InterruptedException {
    if (transactionContextProvider.isTransactional()) {
      transactionContextProvider.getTransactionalContext().clear(queue);
    } else {
      queue.clear();
    }
  }

  @Override
  public boolean offer(Serializable item, long timeout) throws InterruptedException {
    if (transactionContextProvider.isTransactional()) {
      return transactionContextProvider.getTransactionalContext().offer(queue, item, timeout);
    } else {
      return queue.offer(item, 0, timeout);
    }
  }

  @Override
  public Serializable take() throws InterruptedException {
    return poll(Long.MAX_VALUE);
  }

  @Override
  public void untake(Serializable item) throws InterruptedException {
    if (transactionContextProvider.isTransactional()) {
      transactionContextProvider.getTransactionalContext().untake(queue, item);
    } else {
      queue.untake(item);
    }
  }

  @Override
  public Serializable poll(long timeout) throws InterruptedException {
    try {
      if (transactionContextProvider.isTransactional()) {
        Serializable item = transactionContextProvider.getTransactionalContext().poll(queue, timeout);
        return item;
      } else {
        return queue.poll(timeout);
      }
    } catch (InterruptedException iex) {
      if (!deploymentLifecycleState.isStopping()) {
        throw iex;
      }
      // if stopping, ignore
      return null;
    }
  }

  @Override
  public Serializable peek() throws InterruptedException {
    if (transactionContextProvider.isTransactional()) {
      Serializable item = transactionContextProvider.getTransactionalContext().peek(queue);
      return item;
    } else {
      return queue.peek();
    }
  }

  @Override
  public void dispose() throws MuleException, InterruptedException {
    queue.dispose();
  }

  @Override
  public int size() {
    if (transactionContextProvider.isTransactional()) {
      return transactionContextProvider.getTransactionalContext().size(queue);
    } else {
      return queue.getSize();
    }
  }

  @Override
  public String getName() {
    return queue.getName();
  }
}
