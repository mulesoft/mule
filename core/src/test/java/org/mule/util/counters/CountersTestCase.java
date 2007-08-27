/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.counters;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.counters.CounterFactory.Type;

import java.util.Iterator;

public class CountersTestCase extends AbstractMuleTestCase
{

    private static final double delta = 1E-10;

    public void testCreate()
    {
        assertNotNull(CounterFactory.createCounter("create1", Type.NUMBER));
        assertNotNull(CounterFactory.createCounter("create2", Type.NUMBER, false));
        assertNotNull(CounterFactory.createCounter("create3", "create1", Type.MIN));
        assertNotNull(CounterFactory.createCounter("create4", "create1", Type.MIN, false));
        assertNotNull(CounterFactory.createCounter("create5", "create1", "create2", Type.PLUS));
        assertNotNull(CounterFactory.createCounter("create6", "create1", "create2", Type.PLUS, false));

        assertNotNull(CounterFactory.getCounter("create1"));
        assertNull(CounterFactory.getCounter("zzz"));

        for (Iterator it = CounterFactory.getCounters(); it.hasNext();)
        {
            Counter ct = (Counter)it.next();
            assertNotNull(ct);
        }

        try
        {
            CounterFactory.createCounter("create1", Type.NUMBER);
            fail("The creation of a duplicate counter should have failed");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
    }

    public void testNumber()
    {
        Counter ct = CounterFactory.createCounter("testNumber", Type.NUMBER);

        assertEquals("Default value", 0.0, ct.nextValue(), delta);
        ct.setRawValue(10.0);
        assertEquals("setRawValue", 10.0, ct.nextValue(), delta);
        ct.increment();
        assertEquals("increment", 11.0, ct.nextValue(), delta);
        ct.incrementBy(2.0);
        assertEquals("incrementBy", 13.0, ct.nextValue(), delta);
        ct.decrement();
        assertEquals("decrement", 12.0, ct.nextValue(), delta);
    }

    public void testMinMax()
    {
        Counter ct = CounterFactory.createCounter("testMinMax", Type.NUMBER);
        Counter min = CounterFactory.createCounter("testMinMax.min", "testMinMax", Type.MIN);
        Counter max = CounterFactory.createCounter("testMinMax.max", "testMinMax", Type.MAX);

        assertEquals("Min default value", Double.MAX_VALUE, min.nextValue(), delta);
        assertEquals("Max default value", Double.MIN_VALUE, max.nextValue(), delta);
        ct.setRawValue(10.0);
        ct.setRawValue(12.0);
        ct.setRawValue(18.0);
        ct.setRawValue(16.0);
        assertEquals("Min", 10.0, min.nextValue(), delta);
        assertEquals("Max", 18.0, max.nextValue(), delta);
    }

    public void testDelta()
    {
        Counter ct = CounterFactory.createCounter("testDelta", Type.NUMBER);
        Counter dt = CounterFactory.createCounter("testDelta.delta", "testDelta", Type.DELTA);

        assertEquals("Default value", 0.0, dt.nextValue(), delta);
        ct.setRawValue(10.0);
        assertEquals("First value", 10.0, dt.nextValue(), delta);
        ct.setRawValue(12.0);
        assertEquals("Delta", 2.0, dt.nextValue(), delta);
        ct.setRawValue(18.0);
        assertEquals("Delta", 6.0, dt.nextValue(), delta);
        ct.setRawValue(16.0);
        assertEquals("Delta", 0.0, dt.nextValue(), delta);
    }

    public void testSum()
    {
        Counter ct = CounterFactory.createCounter("testSum", Type.NUMBER);
        Counter sum = CounterFactory.createCounter("testSum.sum", "testSum", Type.SUM);

        assertEquals("Default value", 0.0, sum.nextValue(), delta);
        ct.setRawValue(10.0);
        assertEquals("First value", 10.0, sum.nextValue(), delta);
        ct.setRawValue(12.0);
        assertEquals("Sum", 22.0, sum.nextValue(), delta);
        ct.setRawValue(18.0);
        assertEquals("Sum", 40.0, sum.nextValue(), delta);
        ct.setRawValue(16.0);
        assertEquals("Sum", 56.0, sum.nextValue(), delta);
    }

    public void testAverage()
    {
        Counter ct = CounterFactory.createCounter("testAverage", Type.NUMBER);
        Counter avg = CounterFactory.createCounter("testAverage.avg", "testAverage", Type.AVERAGE);

        assertEquals("Default value", 0.0, avg.nextValue(), delta);
        ct.setRawValue(10.0);
        assertEquals("First value", 10.0, avg.nextValue(), delta);
        ct.setRawValue(12.0);
        assertEquals("Average", 11.0, avg.nextValue(), delta);
        ct.setRawValue(18.0);
        assertEquals("Average", 40.0 / 3.0, avg.nextValue(), delta);
        ct.setRawValue(16.0);
        assertEquals("Average", 14.0, avg.nextValue(), delta);
    }

    /**
     * Serious non-portable timing issues :(
     * 
     * @throws InterruptedException
     */
    public void _disabled_testInstantRate() throws InterruptedException
    {
        Counter ct = CounterFactory.createCounter("testRate", Type.NUMBER);
        Counter rate = CounterFactory.createCounter("testRate.rate", "testRate", Type.INSTANT_RATE);

        assertTrue("InstantRate", Double.isNaN(rate.nextValue()));
        Thread.sleep(10);
        ct.setRawValue(1);
        assertTrue("InstantRate", Double.isNaN(rate.nextValue()));
        Thread.sleep(100);
        ct.setRawValue(20);
        assertEquals("InstantRate", 200, rate.nextValue(), 200 * 0.20);
        Thread.sleep(300);
        ct.setRawValue(30);
        assertEquals("InstantRate", 100, rate.nextValue(), 100 * 0.20);
        ct.setRawValue(30);
        ct.setRawValue(30);
        assertTrue("InstantRate", Double.isNaN(rate.nextValue()));
    }

    public void testRatePerUnit() throws InterruptedException
    {
        Counter ct = CounterFactory.createCounter("testRatePerUnit", Type.NUMBER);
        assertNotNull(ct);

        Counter rsec = CounterFactory.createCounter("testRatePerUnit.rate.sec", "testRatePerUnit",
            Type.RATE_PER_SECOND);
        assertNotNull(rsec);

        Counter rmin = CounterFactory.createCounter("testRatePerUnit.rate.min", "testRatePerUnit",
            Type.RATE_PER_MINUTE);
        assertNotNull(rmin);

        assertEquals("Rate", 0.0, rsec.nextValue(), delta);

        for (int i = 0; i < 5; i++)
        {
            ct.setRawValue(10);
            Thread.sleep(1000);
        }

        assertEquals("RatePerSecond", 10.0, rsec.nextValue(), 10.0 * 0.20);
        assertEquals("RatePerMinute", 50.0, rmin.nextValue(), 10.0 * 0.20);
    }
}
