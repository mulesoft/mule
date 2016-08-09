/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueSession;
import org.mule.runtime.core.util.queue.objectstore.xa.AbstractXAResourceManager;
import org.mule.runtime.core.util.queue.objectstore.xa.DefaultXASession;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Queue session that is used to manage the transaction context of a Queue
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
class TransactionalQueueSession extends DefaultXASession implements QueueSession {

  private Logger logger = LoggerFactory.getLogger(org.mule.runtime.core.util.queue.TransactionalQueueSession.class);

  protected TransactionalQueueManager queueManager;

  public TransactionalQueueSession(AbstractXAResourceManager resourceManager, TransactionalQueueManager queueManager) {
    super(resourceManager);
    this.queueManager = queueManager;
  }

  @Override
  public Queue getQueue(String name) {
    QueueInfo queue = queueManager.getQueue(name);
    return new QueueImpl(queue);
  }

  protected class QueueImpl implements Queue {

    protected QueueInfo queue;

    public QueueImpl(QueueInfo queue) {
      this.queue = queue;
    }

    @Override
    public void put(Serializable item) throws InterruptedException, ObjectStoreException {
      offer(item, Long.MAX_VALUE);
    }

    @Override
    public void clear() throws InterruptedException {
      if (localContext != null && !queue.isQueueTransactional()) {
        ((QueueTransactionContext) localContext).clear(queue);
      } else {
        try {
          queueManager.doClear(queue);
        } catch (ObjectStoreException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public boolean offer(Serializable item, long timeout) throws InterruptedException, ObjectStoreException {
      if (localContext != null && !queue.isQueueTransactional()) {
        return ((QueueTransactionContext) localContext).offer(queue, item, timeout);
      } else {
        try {
          Serializable id = queueManager.doStore(queue, item);
          try {
            if (!queue.offer(id, 0, timeout)) {
              queueManager.doRemove(queue, id);
              return false;
            } else {
              return true;
            }
          } catch (InterruptedException e) {
            queueManager.doRemove(queue, id);
            throw e;
          }
        } catch (ObjectStoreException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public Serializable take() throws InterruptedException {
      return poll(Long.MAX_VALUE);
    }

    @Override
    public void untake(Serializable item) throws InterruptedException, ObjectStoreException {
      if (localContext != null && !queue.isQueueTransactional()) {
        ((QueueTransactionContext) localContext).untake(queue, item);
      } else {
        Serializable id = queueManager.doStore(queue, item);
        queue.untake(id);
      }
    }

    @Override
    public Serializable poll(long timeout) throws InterruptedException {
      try {
        if (localContext != null && !queue.isQueueTransactional()) {
          Serializable item = ((QueueTransactionContext) localContext).poll(queue, timeout);
          return postProcessIfNeeded(item);
        } else if (queue.canTakeFromStore()) {
          Serializable item = queue.takeNextItemFromStore(timeout);
          return postProcessIfNeeded(item);
        } else {
          Serializable id = queue.poll(timeout);
          if (id != null) {
            Serializable item = queueManager.doLoad(queue, id);
            if (item != null) {
              queueManager.doRemove(queue, id);
            }
            return postProcessIfNeeded(item);
          }
          return null;
        }
      } catch (InterruptedException iex) {
        if (!queueManager.getMuleContext().isStopping()) {
          throw iex;
        }
        // if stopping, ignore
        return null;
      } catch (ObjectStoreException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Serializable peek() throws InterruptedException {
      try {
        if (localContext != null && !queue.isQueueTransactional()) {
          Serializable item = ((QueueTransactionContext) localContext).peek(queue);
          return postProcessIfNeeded(item);
        } else {
          Serializable id = queue.peek();
          if (id != null) {
            Serializable item = queueManager.doLoad(queue, id);
            return postProcessIfNeeded(item);
          }
          return null;
        }
      } catch (ObjectStoreException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void dispose() throws MuleException, InterruptedException {
      queueManager.disposeQueue(this);
    }

    @Override
    public int size() {
      if (localContext != null && !queue.isQueueTransactional()) {
        return ((QueueTransactionContext) localContext).size(queue);
      } else {
        return queue.getSize();
      }
    }

    @Override
    public String getName() {
      return queue.getName();
    }

    /**
     * Note -- this must handle null items
     */
    private Serializable postProcessIfNeeded(Serializable item) {
      try {
        if (item instanceof DeserializationPostInitialisable) {
          DeserializationPostInitialisable.Implementation.init(item, queueManager.getMuleContext());
        }
        return item;
      } catch (Exception e) {
        logger.warn("Unable to deserialize message", e);
        return null;
      }
    }
  }
}
