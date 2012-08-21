/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of the Lock interface. Useful for doing locking in a single mule instance.
 */
public class ServerLock<T> implements Lock<T>
{
    private Map<T, LockEntry> locks;
    private Object acquireLock = new Object();

    public ServerLock()
    {
        this.locks = new HashMap<T,LockEntry>();
    }

    public void lock(T key)
    {
        LockEntry lock;
        synchronized (acquireLock)
        {
            if (this.locks.containsKey(key))
            {
                lock = this.locks.get(key);
            }
            else
            {
                lock = new LockEntry();
                this.locks.put(key,lock);
            }
            lock.incrementLockCount();
            acquireLock.notifyAll();
        }
        lock.lock();
    }

    public void unlock(T key)
    {
        synchronized (acquireLock)
        {
            LockEntry lock = this.locks.get(key);
            if (lock != null)
            {
                lock.decrementLockCount();
                if (!lock.hasPendingLocks())
                {
                    this.locks.remove(key);
                }
                lock.unlock();
            }
            acquireLock.notifyAll();
        }
    }

    public static class LockEntry
    {
        private AtomicInteger lockCount  = new AtomicInteger(0);
        private ReentrantLock lock = new ReentrantLock(true);

        public void lock()
        {
            lock.lock();
        }

        public void incrementLockCount()
        {
            lockCount.incrementAndGet();
        }

        public void decrementLockCount()
        {
            lockCount.decrementAndGet();
        }

        public void unlock()
        {
            lock.unlock();
        }

        public boolean hasPendingLocks()
        {
            return lockCount.get() > 0;
        }
    }

    @Override
    public void dispose()
    {
        locks.clear();
    }
}
