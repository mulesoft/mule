/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 */
public class FakeLock implements Lock
{

    @Override
    public void lock()
    {
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
    }

    @Override
    public boolean tryLock()
    {
        return false;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        return true;
    }

    @Override
    public void unlock()
    {
    }

    @Override
    public Condition newCondition()
    {
        return null;
    }
};
