/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
