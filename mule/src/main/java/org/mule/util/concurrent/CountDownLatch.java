/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;

/**
 * @author Holger Hoffstaette
 */

public class CountDownLatch extends edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch
    implements Lock
{

    public CountDownLatch(int count)
    {
        super(count);
    }

    public void lock()
    {
        try
        {
            super.await();
        }
        catch (InterruptedException ex)
        {
            // ignore
        }
    }

    public void lockInterruptibly() throws InterruptedException
    {
        super.await();
    }

    public boolean tryLock()
    {
        return (super.getCount() == 0);
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
    {
        return super.await(time, unit);
    }

    public void unlock()
    {
        super.countDown();
    }

    public Condition newCondition()
    {
        throw new UnsupportedOperationException();
    }

}
