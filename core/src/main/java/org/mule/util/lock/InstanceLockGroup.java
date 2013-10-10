/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link LockGroup} implementation for holding references
 * to created locks inside a mule instance.
 */
public class InstanceLockGroup implements LockGroup
{
    private Map<String, LockEntry> locks;
    private Object lockAccessMonitor = new Object();
    private LockProvider lockProvider;

    public InstanceLockGroup(LockProvider lockProvider)
    {
        this.lockProvider = lockProvider;
        this.locks = new HashMap<String,LockEntry>();
    }

    public void lock(String lockId)
    {
        LockEntry lockEntry;
        synchronized (lockAccessMonitor)
        {
            if (locks.containsKey(lockId))
            {
                lockEntry = locks.get(lockId);
            }
            else
            {
                lockEntry = new LockEntry(lockProvider.createLock(lockId));
                locks.put(lockId,lockEntry);
            }
            lockEntry.incrementLockCount();
            lockAccessMonitor.notifyAll();
        }
        lockEntry.getLock().lock();
    }

    public void unlock(String key)
    {
        synchronized (lockAccessMonitor)
        {
            LockEntry lockEntry = locks.get(key);
            if (lockEntry != null)
            {
                lockEntry.decrementLockCount();
                if (!lockEntry.hasPendingLocks())
                {
                    locks.remove(key);
                }
                lockEntry.getLock().unlock();
                lockProvider.destroyLock(lockEntry.getLock());
            }
            lockAccessMonitor.notifyAll();
        }
    }

    public boolean tryLock(String lockId, long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        LockEntry lockEntry;
        synchronized (lockAccessMonitor)
        {
            if (locks.containsKey(lockId))
            {
                lockEntry = locks.get(lockId);
            }
            else
            {
                lockEntry = new LockEntry(lockProvider.createLock(lockId));
                locks.put(lockId,lockEntry);
            }
            lockEntry.incrementLockCount();
            lockAccessMonitor.notifyAll();
        }
        boolean lockAcquired = lockEntry.getLock().tryLock(timeout, timeUnit);
        if (!lockAcquired)
        {
            synchronized (lockAccessMonitor)
            {
                lockEntry.decrementLockCount();
                if (!lockEntry.hasPendingLocks())
                {
                    locks.remove(lockId);
                    lockProvider.destroyLock(lockEntry.getLock());
                }
            }
        }
        return lockAcquired;
    }

    public static class LockEntry
    {
        private AtomicInteger lockCount  = new AtomicInteger(0);
        private Lock lock;
        
        public LockEntry(Lock lock)
        {
            this.lock = lock;
        }

        public Lock getLock()
        {
            return lock;
        }

        public void incrementLockCount()
        {
            lockCount.incrementAndGet();
        }

        public void decrementLockCount()
        {
            lockCount.decrementAndGet();
        }

        public boolean hasPendingLocks()
        {
            return lockCount.get() > 0;
        }
    }

    @Override
    public void dispose()
    {
        synchronized (lockAccessMonitor)
        {
            for (LockEntry lockEntry : locks.values())
            {
                lockProvider.destroyLock(lockEntry.getLock());
            }
            locks.clear();
        }
    }
}
