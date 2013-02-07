/*
 * $Id$
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2010 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
