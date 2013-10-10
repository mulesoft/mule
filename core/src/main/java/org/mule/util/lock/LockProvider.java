/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

/**
 * Provides abstraction in the creation and destruction of Mule locks.
 *
 * {@link LockManager} uses instances of this interface to create and destroy locks.
 *
 * Lock implementation can be changed by replacing the LockProvider in the mule registry.
 */
public interface LockProvider
{
    /**
     * Returns an instance of a {@link Lock}.
     * 
     * @param lockId id that identifies the {@link Lock} instance
     * @return a {@link Lock} instance related to the lockId
     */
    Lock createLock(String lockId);

    /**
     * Destroys a previously created {@link Lock} using {@link #createLock}
     * 
     * @param lock {@link Lock} instance previously created
     */
    void destroyLock(Lock lock);
}
