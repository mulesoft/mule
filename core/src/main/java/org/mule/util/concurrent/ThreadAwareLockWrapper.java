/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.concurrent;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

/**
 * A wrapper for a {@link Lock} which keeps track
 * of the threads that acquire it.
 * <p/>
 * It accounts for the fact that some {@link Lock}
 * implementations allows the same {@link Thread}
 * to acquire the lock several times (e.g: {@link ReadLock}).
 * <p/>
 * Through the {@link #isHeldByCurrentThread()}, this implementation
 * allows to check if the current {@link Thread} has acquired this
 * lock more times than it has released it
 *
 * @since 3.7.0
 */
public class ThreadAwareLockWrapper implements Lock
{

    private Lock delegate;
    private Multiset<Thread> holdingThreads = ConcurrentHashMultiset.create();

    public ThreadAwareLockWrapper(Lock delegate)
    {
        this.delegate = delegate;
    }

    /**
     * @return {@code true} if the current {@link Thread}
     * has {@link #lock()} this lock more times than it has
     * {@link #unlock()} it
     */
    public boolean isHeldByCurrentThread()
    {
        return holdingThreads.count(Thread.currentThread()) > 0;
    }

    @Override
    public void lock()
    {
        delegate.lock();
        registerHoldingThread();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
        delegate.lockInterruptibly();
        registerHoldingThread();
    }

    @Override
    public boolean tryLock()
    {
        if (delegate.tryLock())
        {
            registerHoldingThread();
            return true;
        }

        return false;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException
    {
        if (delegate.tryLock(timeout, unit))
        {
            registerHoldingThread();
            return true;
        }

        return false;
    }

    @Override
    public void unlock()
    {
        delegate.unlock();
        unregisterHoldingThread();
    }

    @Override
    public Condition newCondition()
    {
        return delegate.newCondition();
    }

    private void registerHoldingThread()
    {
        holdingThreads.add(Thread.currentThread());
    }

    private void unregisterHoldingThread()
    {
        holdingThreads.remove(Thread.currentThread());
    }
}
