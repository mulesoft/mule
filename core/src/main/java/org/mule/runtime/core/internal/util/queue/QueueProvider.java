/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

/**
 * Provides access to a certain queue.
 */
public interface QueueProvider {

  /**
   * @param queueName queue name
   * @return the queue store related to the queue with queueName
   */
  QueueStore getQueue(String queueName);

  /**
   * @param queueName queue name
   * @return a recovery queue store for the queue with queueName
   */
  RecoverableQueueStore getRecoveryQueue(String queueName);

}
