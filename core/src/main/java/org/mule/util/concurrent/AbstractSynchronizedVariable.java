/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
