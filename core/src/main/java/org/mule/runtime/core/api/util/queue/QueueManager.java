/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.util.queue;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

import java.util.Optional;

/**
 * A Queue manager is responsible for managing one or more Queue resources and providing common support for transactions and
 * persistence.
 */
public interface QueueManager extends Startable, Stoppable {

  /**
   * Returns a new instance of {@link QueueSession} bounded to this {@link QueueManager}
   *
   * @return session for retrieving queues and handle transactions
   */
  QueueSession getQueueSession();

  /**
   * Sets the default {@link QueueConfiguration} for any created {@link Queue} for which a custom
   * configuration hasn't been specified
   *
   * @param config an instance of {@link QueueConfiguration}
   */
  void setDefaultQueueConfiguration(QueueConfiguration config);

  /**
   * Specifies a {@link QueueConfiguration} for the queue which name matches queueName
   *
   * @param queueName the name of a {@link Queue}
   * @param config    an instance of {@link QueueConfiguration}
   */
  void setQueueConfiguration(String queueName, QueueConfiguration config);

  /**
   * Returns the configuration of the {@link Queue} of the given {@code queueName}
   *
   * @param queueName the name of a configured queue
   * @return the queue's configuration or {@link Optional#empty()} if no such queue
   */
  Optional<QueueConfiguration> getQueueConfiguration(String queueName);
}
