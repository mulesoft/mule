/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation for a QueueManager.
 *
 * Contains all the logic related to queue caching and queue configuration definition.
 */
public abstract class AbstractQueueManager
    implements QueueManager, QueueProvider, QueueStoreCacheListener, MuleContextAware, Initialisable, Disposable {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private final ReentrantLock queuesLock = new ReentrantLock();
  private final Map<String, CacheAwareQueueStore> queues = new HashMap<>();
  private final Map<String, QueueConfiguration> queueConfigurations = new HashMap<>();
  private QueueConfiguration defaultQueueConfiguration = new DefaultQueueConfiguration();
  private MuleContext muleContext;

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setDefaultQueueConfiguration(QueueConfiguration config) {
    this.defaultQueueConfiguration = config;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setQueueConfiguration(String queueName, QueueConfiguration newConfig) {
    // We allow calling this method only if the new config is equals to the previous one because of MULE-7420
    if (queues.containsKey(queueName) && !newConfig.equals(queueConfigurations.get(queueName))) {
      throw new MuleRuntimeException(CoreMessages.createStaticMessage(String
          .format("A queue with name %s is in use so we cannot change it's configuration", queueName)));
    }
    if (logger.isDebugEnabled()) {
      if (queueConfigurations.containsKey(queueName)) {
        QueueConfiguration oldConfiguration = queueConfigurations.get(queueName);
        logger.debug(String.format("Replacing queue %s configuration: %s with new newConfig: %s", queueName, oldConfiguration,
                                   newConfig));
      }
    }
    queueConfigurations.put(queueName, newConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<QueueConfiguration> getQueueConfiguration(String queueName) {
    return ofNullable(queueConfigurations.get(queueName));
  }

  private QueueStore getQueue(String name, QueueConfiguration config) {
    CacheAwareQueueStore queueStore = queues.get(name);
    if (queueStore != null) {
      return queueStore;
    }
    queuesLock.lock();
    try {
      queueStore = queues.get(name);
      if (queueStore == null) {
        queueStore = new CacheAwareQueueStore(createQueueStore(name, config), this);
        queues.put(name, queueStore);
      }

      return queueStore;
    } finally {
      queuesLock.unlock();
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public final void dispose() {
    doDispose();
    queues.clear();
  }

  @Override
  public void disposeQueueStore(QueueStore queueStore) {
    removeQueueFromCache(queueStore);
  }

  @Override
  public void closeQueueStore(QueueStore queueStore) {
    removeQueueFromCache(queueStore);
  }

  private void removeQueueFromCache(QueueStore queueStore) {
    try {
      if (queueStore == null) {
        throw new IllegalArgumentException("Queue to be disposed cannot be null");
      }
      final String queueName = queueStore.getName();
      queuesLock.lock();
      try {
        if (!this.queues.containsKey(queueName)) {
          throw new IllegalArgumentException(String.format("There's no queue for name %s", queueName));
        }
        this.queues.remove(queueName);
      } finally {
        queuesLock.unlock();
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public synchronized QueueStore getQueue(String queueName) {
    return getQueue(queueName, defineQueueConfiguration(queueName));
  }

  private QueueConfiguration defineQueueConfiguration(String queueName) {
    if (!queueConfigurations.containsKey(queueName)) {
      setQueueConfiguration(queueName, defaultQueueConfiguration);
      return defaultQueueConfiguration;
    } else {
      return queueConfigurations.get(queueName);
    }
  }

  protected void clearQueueConfiguration(String queueName) {
    this.queueConfigurations.remove(queueName);
  }


  /**
   * Creates a QueueStore
   *
   * @param name queue name
   * @param config configuration for the queue
   * @return a new QueueStore for the given queue name
   */
  protected abstract QueueStore createQueueStore(String name, QueueConfiguration config);

  /**
   * Dispose resources allocated by the implementations.
   *
   * This method is run after the queues were disposed and removed from the cache.
   */
  protected abstract void doDispose();

}
