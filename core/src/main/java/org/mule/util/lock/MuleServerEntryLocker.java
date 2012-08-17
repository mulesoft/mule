/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.lock;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MuleServerEntryLocker implements MuleEntryLocker<Serializable>
{
    private Map<Serializable, ReentrantLock> locks;
    private Object acquireLock = new Object();

    public MuleServerEntryLocker()
    {
        this.locks = new HashMap<Serializable,ReentrantLock>();
    }

    public void lock(Serializable key)
    {
        ReentrantLock lock;
        synchronized (acquireLock)
        {
            if (this.locks.containsKey(key))
            {
                lock = this.locks.get(key);
            }
            else
            {
                lock = new ReentrantLock(true);
                this.locks.put(key,lock);
            }
        }
        lock.lock();
    }

    public void release(Serializable key)
    {
        synchronized (acquireLock)
        {
            ReentrantLock reentrantLock = this.locks.get(key);
            if (reentrantLock != null)
            {
                if (!reentrantLock.hasQueuedThreads())
                {
                    this.locks.remove(key);
                }
                reentrantLock.unlock();
            }
        }
    }
}
