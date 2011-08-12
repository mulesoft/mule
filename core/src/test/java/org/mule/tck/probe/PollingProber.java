/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.probe;

/**
 * Implements {@link Prober} using a polling mechanism that evaluates
 * a {@link Probe} until is satisfied or a timeout is reached.
 */
public class PollingProber implements Prober
{

    public static final long DEFAULT_TIMEOUT = 1000;
    public static final long DEFAULT_POLLING_INTERVAL = 100;

    private final long timeoutMillis;
    private final long pollDelayMillis;
    private final boolean startWithDelay;

    public PollingProber() {
        this(DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL);
    }

    public PollingProber(long timeoutMillis, long pollDelayMillis)
    {
        this.timeoutMillis = timeoutMillis;
        this.pollDelayMillis = pollDelayMillis;
        this.startWithDelay = false;
    }
    
    public PollingProber(long timeoutMillis, long pollDelayMillis, boolean startWithDelay)
    {
        this.timeoutMillis = timeoutMillis;
        this.pollDelayMillis = pollDelayMillis;
        this.startWithDelay = startWithDelay;
    }

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
        boolean doCheck = true;
                
        if (startWithDelay)
        {
            doCheck = false;
        }
        
        while (true)
        {
            if (doCheck && probe.isSatisfied())
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
            
            doCheck = true;
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
