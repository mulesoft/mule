/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.model.seda.SedaService;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

public abstract class AbstractThreadSafeAccessTestCase extends AbstractMuleContextTestCase
{
    protected ThreadSafeAccess dummyEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Object(), (Map) null, muleContext);
        return new DefaultMuleEvent(message, MuleTestUtils.getTestInboundEndpoint("test",
            MessageExchangePattern.ONE_WAY, muleContext, null), new SedaService(muleContext));
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

        public void run()
        {
            try
            {
                for (int i = 0; i < write.length; i++)
                {
                    target.assertAccess(write[i]);
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
