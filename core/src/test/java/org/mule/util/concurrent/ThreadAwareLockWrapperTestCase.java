/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.locks.Lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ThreadAwareLockWrapperTestCase extends AbstractMuleTestCase
{

    private ThreadAwareLockWrapper lock;

    @Mock
    private Lock delegate;

    @Before
    public void before()
    {
        lock = new ThreadAwareLockWrapper(delegate);
    }

    @Test
    public void lockAndUnlockByCurrentThread()
    {
        lock.lock();
        assertHeldByCurrentThread(true);
        verify(delegate).lock();

        lock.unlock();
        assertHeldByCurrentThread(false);
        verify(delegate).unlock();
    }

    @Test
    public void lockManyTimes()
    {
        lock.lock();
        lock.lock();
        assertHeldByCurrentThread(true);

        lock.unlock();
        assertHeldByCurrentThread(true);

        lock.unlock();
        assertHeldByCurrentThread(false);
    }

    @Test
    public void successfulTryLock()
    {
        assertTryLock(true);
    }

    @Test
    public void failingTryLock()
    {
        assertTryLock(false);
    }

    @Test
    public void isThreadAware() throws Exception
    {
        final Latch latch = new Latch();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                lock.lock();
                latch.release();
            }
        }).start();

        latch.await();
        assertHeldByCurrentThread(false);
    }

    private void assertHeldByCurrentThread(boolean isHeld)
    {
        assertThat(lock.isHeldByCurrentThread(), is(isHeld));
    }

    private void assertTryLock(boolean canLock)
    {
        when(delegate.tryLock()).thenReturn(canLock);
        assertThat(lock.tryLock(), is(canLock));
        assertHeldByCurrentThread(canLock);
    }


}
