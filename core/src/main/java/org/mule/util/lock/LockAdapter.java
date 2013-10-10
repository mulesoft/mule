/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link Lock} that delegates the locking mechanism to
 * a {@link LockGroup} but looks like a regular lock from the client's perspective
 */
public class LockAdapter implements Lock
{
    private LockGroup lockGroup;
    private String lockId;

    public LockAdapter(String lockId, LockGroup lockGroup)
    {
        this.lockGroup = lockGroup;
        this.lockId = lockId;
    }

    @Override
    public void lock()
    {
        lockGroup.lock(lockId);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        return lockGroup.tryLock(lockId, timeout, timeUnit);
    }

    @Override
    public void unlock()
    {
        lockGroup.unlock(lockId);
    }
}
