/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.util.xa.ResourceManagerSystemException;

/**
 * A Queue manager is responsible for manageing one or more Queue resources and
 * providing common support fot transactions and persistence
 */
public interface QueueManager
{

    void start() throws ResourceManagerSystemException;

    void stop() throws ResourceManagerSystemException;

    QueueSession getQueueSession();

    void close();

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

    QueuePersistenceStrategy getMemoryPersistenceStrategy();

    void setMemoryPersistenceStrategy(QueuePersistenceStrategy memoryPersistenceStrategy);

}
