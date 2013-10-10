/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import java.util.concurrent.Executor;

// @ThreadSafe
public abstract class AbstractSynchronizedVariable implements Executor
{
    // @GuardedBy(itself)
    protected final Object lock;

    public AbstractSynchronizedVariable()
    {
        super();
        lock = this;
    }

    public AbstractSynchronizedVariable(Object lock)
    {
        super();
        this.lock = lock;
    }

    public Object getLock()
    {
        return lock;
    }

    public void execute(Runnable command)
    {
        synchronized (lock)
        {
            command.run();
        }
    }

}
