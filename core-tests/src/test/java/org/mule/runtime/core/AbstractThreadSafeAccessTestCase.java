/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.ThreadSafeAccess;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

public abstract class AbstractThreadSafeAccessTestCase extends AbstractMuleContextTestCase
{
    protected ThreadSafeAccess dummyEvent() throws Exception
    {
        return new DefaultMuleEvent(MuleMessage.builder().payload(new Object()).build(), getTestFlow());
    }

    protected void resetAccessControl(ThreadSafeAccess target) throws InterruptedException
    {
        target.assertAccess(true);
        newThread(target, true, new boolean[]{true});
        target.resetAccessControl();
        newThread(target, false, new boolean[]{true});
    }

    protected void basicPattern(ThreadSafeAccess target) throws InterruptedException
    {
        newThread(target, false, new boolean[]{true, true, false, true});
        newThread(target, false, new boolean[]{false});
        newThread(target, true, new boolean[]{true});
    }

    protected void newCopy(ThreadSafeAccess target) throws InterruptedException
    {
        basicPattern(target);
        basicPattern(target.newThreadCopy());
    }

    protected void newThread(ThreadSafeAccess target, boolean error, boolean[] pattern) throws InterruptedException
    {
        Caller caller = new Caller(target, pattern);
        Thread thread =  new Thread(caller);
        thread.start();
        thread.join();
        assertEquals(error, caller.isError());
    }

    protected static class Caller implements Runnable
    {

        private boolean isError = false;
        private ThreadSafeAccess target;
        private boolean[] write;

        public Caller(ThreadSafeAccess target, boolean[] write)
        {
            this.target = target;
            this.write = write;
        }

        @Override
        public void run()
        {
            try
            {
                for (boolean element : write)
                {
                    target.assertAccess(element);
                }
            }
            catch (IllegalStateException e)
            {
                isError = true;
            }
        }

        public boolean isError()
        {
            return isError;
        }
    }
}
