/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
