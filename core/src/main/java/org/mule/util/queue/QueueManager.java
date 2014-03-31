/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * A Queue manager is responsible for managing one or more Queue resources and
 * providing common support for transactions and persistence.
 */
public interface QueueManager extends Startable, Stoppable
{
    /**
     * Returns a new instance of {@link QueueSession} bounded to this
     * {@link QueueManager}
     * 
     * @return session for retrieving queues and handle transactions
     */
    QueueSession getQueueSession();

    /**
     * Sets the default {@link org.mule.util.queue.QueueConfiguration} for any created {@link Queue} for
     * which a custom configuration hasn't been specified
     * 
     * @param config an instance of {@link org.mule.util.queue.QueueConfiguration}
     */
    void setDefaultQueueConfiguration(QueueConfiguration config);

    /**
     * Specifies a {@link org.mule.util.queue.QueueConfiguration} for the queue which name matches
     * queueName
     * 
     * @param queueName the name of a {@link Queue}
     * @param config an instance of {@link org.mule.util.queue.QueueConfiguration}
     */
    void setQueueConfiguration(String queueName, QueueConfiguration config);
}
