/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

import java.util.concurrent.TimeUnit;

/**
 * Lock to be used for synchronization of access in Mule Components.
 */
public interface Lock
{
    /**
     * Acquires the lock. Blocks until the lock is acquired
     */
    void lock();

    /**
     * Tries to acquire the lock for a certain amount of time
     *
     * @param timeout the time in timeUnit to wait until the lock is acquired
     * @param timeUnit the time unit of timeout
     * @return true if the lock was successfully acquired, false otherwise
     * @throws java.lang.InterruptedException if thread was interrupted during the lock acquisition
     */
    boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * Release the lock.
     */
    void unlock();
}
