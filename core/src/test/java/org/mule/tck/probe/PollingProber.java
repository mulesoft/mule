/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.probe;

public class PollingProber implements Prober
{
    public static final long DEFAULT_TIMEOUT = 1000;
    public static final long DEFAULT_POLLING_INTERVAL = 100;

    private final long timeoutMillis;
    private final long pollDelayMillis;

    public PollingProber()
    {
        this(DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL);
    }

    public PollingProber(long timeoutMillis, long pollDelayMillis)
    {
        this.timeoutMillis = timeoutMillis;
        this.pollDelayMillis = pollDelayMillis;
    }

    @Override
    public void check(Probe probe)
    {
        if (!poll(probe))
        {
            throw new AssertionError(probe.describeFailure());
        }
    }

    private boolean poll(Probe probe)
    {
        Timeout timeout = new Timeout(timeoutMillis);

        while (true)
        {
            if (probe.isSatisfied())
            {
                return true;
            }
            else if (timeout.hasTimedOut())
            {
                return false;
            }
            else
            {
                waitFor(pollDelayMillis);
            }
        }
    }

    private void waitFor(long duration)
    {
        try
        {
            Thread.sleep(duration);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("unexpected interrupt", e);
        }
    }
}
