/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class InstantRate extends AggregateCounter
{

    private double firstTime;
    private double lastTime;
    private double value;

    public InstantRate(String name, AbstractCounter base)
    {
        super(name, Type.INSTANT_RATE, base);
    }

    public double nextValue()
    {
        if (firstTime == 0 || firstTime == lastTime)
        {
            return Double.NaN;
        }
        else
        {
            return value / (lastTime - firstTime) * 1000.0;
        }
    }

    public void doCompute()
    {
        firstTime = lastTime;
        lastTime = System.currentTimeMillis();
        value = getBase().nextValue();
    }

}
