/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link LockProvider} implementation for applications running in a single mule server
 */
public class SingleServerLockProvider implements LockProvider
{
    @Override
    public Lock createLock(String lockId)
    {
        return new Lock()
        {
            private ReentrantLock reentrantLock = new ReentrantLock(true);
            
            @Override
            public void lock()
            {
                reentrantLock.lock();
            }

            @Override
            public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException
            {
                return reentrantLock.tryLock(timeout,timeUnit);
            }

            @Override
            public void unlock()
            {
                reentrantLock.unlock();
            }
        };
    }

    @Override
    public void destroyLock(Lock lock)
    {
        //Nothing to do.
    }
}
