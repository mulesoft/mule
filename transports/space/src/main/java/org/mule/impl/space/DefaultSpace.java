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

import java.util.Iterator;
import java.util.List;

import org.mule.umo.space.UMOSpaceException;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.QueueSession;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.xa.ResourceManagerException;
import org.mule.util.xa.ResourceManagerSystemException;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates a transaction and persistent local space. This should not be used in a
 * clustered environment
 */
public class DefaultSpace extends AbstractSpace
{

    private Queue space;
    private QueueSession session;
    private QueueManager queueManager;
    private boolean localQueueManager = false;
    private List children = new CopyOnWriteArrayList();
    private String parentName;

    DefaultSpace(String name, QueuePersistenceStrategy ps, int capacity)
        throws ResourceManagerSystemException
    {
        this(name, ps, true, capacity);
    }

    public DefaultSpace(String name, QueuePersistenceStrategy ps, boolean enableMonitorEvents, int capacity)
        throws ResourceManagerSystemException
    {
        super(name, enableMonitorEvents);
        queueManager = new TransactionalQueueManager();
        localQueueManager = true;
        queueManager.setPersistenceStrategy(ps);
        queueManager.setQueueConfiguration(name, new QueueConfiguration(capacity, !ps.isTransient()));
        session = queueManager.getQueueSession();
        space = session.getQueue(name);
        queueManager.start();
    }

    public DefaultSpace(String name, QueueManager qm, boolean enableMonitorEvents)
    {
        super(name, enableMonitorEvents);
        queueManager = qm;
        session = queueManager.getQueueSession();
        space = session.getQueue(name);
    }

    /**
     * Used for creating child spaces where the session sohuld be reused
     * 
     * @param name
     * @param queueManager
     * @param session
     * @param enableMonitorEvents
     */
    protected DefaultSpace(String name,
                           String parentName,
                           QueueManager queueManager,
                           QueueSession session,
                           boolean enableMonitorEvents)
    {
        super(parentName + "." + name, enableMonitorEvents);
        this.queueManager = queueManager;
        this.session = session;
        space = session.getQueue(parentName);
        localQueueManager = false;
        this.parentName = parentName;
    }

    public void doPut(Object item) throws UMOSpaceException
    {
        try
        {
            space.put(item);
        }
        catch (InterruptedException e)
        {
            // throw new SpaceActionInteruptedException("put", e);
        }
    }

    public void doPut(Object item, long lease) throws UMOSpaceException
    {
        try
        {
            space.put(item);
        }
        catch (InterruptedException e)
        {
            // throw new SpaceActionInteruptedException("put-lease", e);
        }
    }

    public Object doTake() throws UMOSpaceException
    {
        try
        {
            return space.take();
        }
        catch (InterruptedException e)
        {
            // throw new SpaceActionInteruptedException("take", e);
            return null;
        }
    }

    public Object doTake(long timeout) throws UMOSpaceException
    {
        try
        {
            return space.poll(timeout);
        }
        catch (InterruptedException e)
        {
            // throw new SpaceActionInteruptedException("take-" + timeout, e);
            return null;
        }
    }

    public Object doTakeNoWait() throws UMOSpaceException
    {
        try
        {
            return space.poll(1L);
        }
        catch (InterruptedException e)
        {
            // throw new SpaceActionInteruptedException("takeNoWait", e);
            return null;
        }
    }

    protected void doDispose()
    {
        for (Iterator iterator = children.iterator(); iterator.hasNext();)
        {
            DefaultSpace childSpace = (DefaultSpace)iterator.next();
            childSpace.dispose();
        }
        children.clear();
        if (localQueueManager)
        {
            try
            {
                queueManager.stop();
            }
            catch (ResourceManagerSystemException e)
            {
                logger.warn(e);
            }
        }
    }

    public int size()
    {
        return space.size();
    }

    public void beginTransaction() throws UMOSpaceException
    {
        try
        {
            session.begin();
        }
        catch (ResourceManagerException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public void commitTransaction() throws UMOSpaceException
    {
        try
        {
            session.commit();
        }
        catch (ResourceManagerException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public void rollbackTransaction() throws UMOSpaceException
    {
        try
        {
            session.rollback();
        }
        catch (ResourceManagerException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public DefaultSpace createChild(String name)
    {
        DefaultSpace child = new DefaultSpace(name, (parentName == null ? this.name : parentName),
            queueManager, session, enableMonitorEvents);
        logger.info("created child space: " + child.getName());
        children.add(child);
        return child;
    }

    public String getParentName()
    {
        return parentName;
    }
}
