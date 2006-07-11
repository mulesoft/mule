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
// @ThreadSafe
public class CountDownLatch extends edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch
    implements Lock
{

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch#CountDownLatch(int)
     */
    public CountDownLatch(int count)
    {
        super(count);
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#lock()
     */
    public void lock()
    {
        boolean interrupted = false;

        try
        {
            while (super.getCount() > 0)
            {
                try
                {
                    super.await();
                }
                catch (InterruptedException ex)
                {
                    interrupted = true;
                }
            }
        }
        finally
        {
            if (interrupted)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#lockInterruptibly()
     */
    public void lockInterruptibly() throws InterruptedException
    {
        super.await();
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#tryLock()
     */
    public boolean tryLock()
    {
        return (super.getCount() == 0);
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#tryLock(long,
     *      TimeUnit)
     */
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
    {
        return super.await(time, unit);
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#unlock()
     */
    public void unlock()
    {
        super.countDown();
    }

    /**
     * @see edu.emory.mathcs.backport.java.util.concurrent.locks.Lock#newCondition()
     * @throws UnsupportedOperationException
     */
    public Condition newCondition()
    {
        throw new UnsupportedOperationException();
    }

}
