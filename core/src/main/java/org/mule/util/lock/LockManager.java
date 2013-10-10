/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

/**
 * Factory for creating Lock instances.
 *
 * All mule components that require synchronization for access shared data must be synchronized using locks from
 * the LockManager provided by MuleContext
 *
 * Default LockManager can be override by modules using registry-bootstrap.
 */
public interface LockManager
{

    /**
     * Provides a lock to create thread safe Mule components.
     *
     * Always returns the same lock for a certain lockId
     *
     * @param lockId Id of the lock
     * @return a {@link Lock} instance associated to the lockId
     */
    Lock getLock(String lockId);
    
}
