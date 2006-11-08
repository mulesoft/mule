/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.space;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.umo.space.UMOSpaceFactory;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.xa.ResourceManagerSystemException;

/**
 * As Space factory that creates a local, non-shared space. If a Jounaling or DB
 * persistence strategy is used this can be used in a clustered environment
 */
public abstract class DefaultSpaceFactory implements UMOSpaceFactory
{
    private QueuePersistenceStrategy persistenceStrategy;
    private QueueManager queueManager;
    private boolean enableMonitorEvents = true;
    private int capacity = 0;
    private boolean enableCaching = false;

    public DefaultSpaceFactory()
    {
        super();
    }

    public DefaultSpaceFactory(boolean enableMonitorEvents)
    {
        this.enableMonitorEvents = enableMonitorEvents;
    }

    public DefaultSpaceFactory(boolean enableMonitorEvents, int capacity)
    {
        this.enableMonitorEvents = enableMonitorEvents;
        this.capacity = capacity;
    }

    public DefaultSpaceFactory(QueuePersistenceStrategy ps, boolean enableMonitorEvents)
    {
        this.persistenceStrategy = ps;
        this.enableMonitorEvents = enableMonitorEvents;
    }

    public DefaultSpaceFactory(QueuePersistenceStrategy ps, boolean enableMonitorEvents, int capacity)
    {
        this.persistenceStrategy = ps;
        this.enableMonitorEvents = enableMonitorEvents;
        this.capacity = capacity;
    }

    public DefaultSpaceFactory(QueueManager qm, boolean enableMonitorEvents)
    {
        this.queueManager = qm;
        this.enableMonitorEvents = enableMonitorEvents;
    }

    public boolean isEnableMonitorEvents()
    {
        return enableMonitorEvents;
    }

    public void setEnableMonitorEvents(boolean enableMonitorEvents)
    {
        this.enableMonitorEvents = enableMonitorEvents;
    }

    public int getCapacity()
    {
        return capacity;
    }

    public void setCapacity(int capacity)
    {
        this.capacity = capacity;
    }

    public QueuePersistenceStrategy getPersistenceStrategy()
    {
        return persistenceStrategy;
    }

    public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy)
    {
        this.persistenceStrategy = persistenceStrategy;
    }

    public QueueManager getQueueManager()
    {
        return queueManager;
    }

    public void setQueueManager(QueueManager queueManager)
    {
        this.queueManager = queueManager;
    }

    public boolean isEnableCaching()
    {
        return enableCaching;
    }

    public void setEnableCaching(boolean enableCaching)
    {
        this.enableCaching = enableCaching;
    }

    public UMOSpace create(String name) throws UMOSpaceException
    {

        if (capacity == 0)
        {
            capacity = 1024;
        }
        try
        {
            if (queueManager != null)
            {
                return new DefaultSpace(name, queueManager, enableMonitorEvents);
            }
            else if (persistenceStrategy == null)
            {
                persistenceStrategy = new MemoryPersistenceStrategy();
            }
            if (enableCaching)
            {
                persistenceStrategy = new CachingPersistenceStrategy(persistenceStrategy);
            }
            return new DefaultSpace(name, persistenceStrategy, enableMonitorEvents, capacity);

        }
        catch (ResourceManagerSystemException e)
        {
            throw new CreateSpaceException(e);
        }
    }

    /**
     * Creates a space based on the endpoint URI and can use additional properties,
     * transaction info, security info and filters associated with the endpoint
     * 
     * @param endpoint the endpoint from which to construct the space
     * @return an new Space object
     * @throws org.mule.umo.space.UMOSpaceException
     */
    public UMOSpace create(UMOImmutableEndpoint endpoint) throws UMOSpaceException
    {
        return create(endpoint.getEndpointURI().getAddress());
    }
}
