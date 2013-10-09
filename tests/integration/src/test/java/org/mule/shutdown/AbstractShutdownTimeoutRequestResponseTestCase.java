/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.shutdown;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractShutdownTimeoutRequestResponseTestCase extends FunctionalTestCase
{

    protected static int WAIT_TIME = 2000;
    protected static Latch waitLatch;

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Before
    public void setUpWaitLatch() throws Exception
    {
        waitLatch = new Latch();
    }

    private static class BlockMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            waitLatch.release();

            try
            {
                Thread.sleep(WAIT_TIME);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new DefaultMuleException(e);
            }

            return event;
        }
    }
}
