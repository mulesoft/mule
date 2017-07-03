/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
