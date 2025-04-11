/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueSession;

public abstract class AbstractQueueSession implements QueueSession {

  private final QueueProvider queueProvider;
  private final LifecycleState deploymentLifecycleState;

  protected AbstractQueueSession(QueueProvider queueProvider,
                                 LifecycleState deploymentLifecycleState) {
    this.queueProvider = queueProvider;
    this.deploymentLifecycleState = deploymentLifecycleState;
  }

  @Override
  public Queue getQueue(String name) {
    QueueStore queueStore = queueProvider.getQueue(name);
    return new TransactionAwareQueueStore(queueStore, new TransactionContextProvider() {

      @Override
      public boolean isTransactional() {
        return getTransactionalContext() != null;
      }

      @Override
      public QueueTransactionContext getTransactionalContext() {
        return AbstractQueueSession.this.getTransactionalContext();
      }
    }, deploymentLifecycleState);
  }

  protected QueueProvider getQueueProvider() {
    return queueProvider;
  }

  protected abstract QueueTransactionContext getTransactionalContext();
}
