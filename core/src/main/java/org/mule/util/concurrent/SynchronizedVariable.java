/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

// @ThreadSafe
public abstract class SynchronizedVariable implements Executor
{
    // @GuardedBy(itself)
    protected final Object _lock;

    public SynchronizedVariable()
    {
        super();
        _lock = this;
    }

    public SynchronizedVariable(Object lock)
    {
        super();
        _lock = lock;
    }

    public Object getLock()
    {
        return _lock;
    }

    public void execute(Runnable command)
    {
        synchronized (_lock)
        {
            command.run();
        }
    }

}
