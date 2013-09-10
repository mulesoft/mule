/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConcurrentWorkTracker implements WorkTracker
{

    protected static final Log logger = LogFactory.getLog(ConcurrentWorkTracker.class);

    private final ReadWriteLock registryLock = new ReentrantReadWriteLock();
    private final List<Runnable> works = new LinkedList<Runnable>();

    public List<Runnable> pendingWorks()
    {
        Lock lock = registryLock.readLock();

        try
        {
            lock.lock();
            return Collections.unmodifiableList(works);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void addWork(Runnable work)
    {
        Lock lock = registryLock.writeLock();

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Tracking work: " + work);
            }
            lock.lock();
            works.add(work);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void removeWork(Runnable work)
    {
        Lock lock = registryLock.writeLock();

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Untracking work: " + work);
            }

            lock.lock();
            works.remove(work);
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void dispose()
    {
        Lock lock = registryLock.writeLock();

        try
        {
            lock.lock();
            works.clear();
        }
        finally
        {
            lock.unlock();
        }
    }
}