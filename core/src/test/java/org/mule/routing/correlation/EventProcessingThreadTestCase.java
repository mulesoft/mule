/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.correlation;

import org.mule.routing.EventProcessingThread;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class EventProcessingThreadTestCase extends AbstractMuleTestCase
{

    @Test
    public void survivesProcessRuntimeExceptions()
    {
        final TestEventProcessingThread processingThread = new TestEventProcessingThread("testEventProcessingThread", 1);
        try
        {
            processingThread.start();
            Prober prober = new PollingProber(100, 1);
            prober.check(new Probe()
            {
                public boolean isSatisfied()
                {
                    return processingThread.count > 1;
                }

                public String describeFailure()
                {
                    return "Expected more than one invocation of the thread processing method";
                }
            });
        }
        finally
        {
            processingThread.interrupt();
        }
    }

    private static class TestEventProcessingThread extends EventProcessingThread
    {

        volatile int count;

        public TestEventProcessingThread(String name, long delayTime)
        {
            super(name, delayTime);
        }

        @Override
        protected void doRun()
        {
            count++;
            throw new RuntimeException();
        }
    }
}
