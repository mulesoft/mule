/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.fail;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class AbstractQuartzStatefulTestCase extends FunctionalTestCase
{

    protected void assertOnlyOneThreadWaiting(final List<String> messages, CountDownLatch latch)
    {
        boolean failure = false;

        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);
        try
        {
            prober.check(new Probe()
            {
                private final int count = 1;

                @Override
                public boolean isSatisfied()
                {
                    synchronized (messages)
                    {
                        return messages.size() > count;
                    }
                }

                @Override
                public String describeFailure()
                {
                    return "Did not receive the expected number of messages";
                }
            });

            failure = true;
        }
        catch (AssertionError e)
        {
            // Perfect: only one thread was executing the flow
        }

        // Unblock any awaiting thread
        latch.countDown();

        if (failure)
        {
            fail("Only one thread should be executing a stateful quartz job");
        }
    }

}
