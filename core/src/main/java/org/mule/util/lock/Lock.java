/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
