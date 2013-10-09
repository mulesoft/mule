/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

}