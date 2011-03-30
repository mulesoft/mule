/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
    QueueSession getQueueSession();

    void setDefaultQueueConfiguration(QueueConfiguration config);

    void setQueueConfiguration(String queueName, QueueConfiguration config);

    /**
     * @return Returns the persistenceStrategy.
     */
    QueuePersistenceStrategy getPersistenceStrategy();

    /**
     * @param persistenceStrategy The persistenceStrategy to set.
     */
    void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy);

    /**
     * @deprecated QueueManager should not be able to choose different persistence strategies,
     * it should use whatever is set via #setPersistenceStrategy
     */
    @Deprecated
    QueuePersistenceStrategy getMemoryPersistenceStrategy();

    /**
     * @deprecated QueueManager should not be able to choose different persistence strategies,
     * it should use whatever is set via #setPersistenceStrategy
     */
    @Deprecated
    void setMemoryPersistenceStrategy(QueuePersistenceStrategy memoryPersistenceStrategy);
}
