/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.UUID;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UUIDPerformanceTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Override
    public int getTestTimeoutSecs()
    {
        return 360;
    }

    @Test
    @PerfTest(duration = 30000, threads = 1, warmUp = 5000)
    public void singleThread() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

    @Test
    @PerfTest(duration = 30000, threads = 10, warmUp = 5000)
    public void tenThreads() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

    @Test
    @PerfTest(duration = 30000, threads = 20, warmUp = 5000)
    public void twentyThreads() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

    @Test
    @PerfTest(duration = 30000, threads = 50, warmUp = 5000)
    public void fivtyThreads() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

    @Test
    @PerfTest(duration = 30000, threads = 100, warmUp = 5000)
    public void hundredThreads() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

    @Test
    @PerfTest(duration = 30000, threads = 200, warmUp = 5000)
    public void twoHundredThreads() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            UUID.getUUID();
        }
    }

}
