/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class TimeAverage extends AggregateCounter
{

    private double sum = 0.0;
    private double lastValue = 0.0;
    private final long firstTime = System.currentTimeMillis();
    private long lastTime = firstTime;

    public TimeAverage(String name, AbstractCounter base)
    {
        super(name, Type.AVERAGE, base);
    }

    public double nextValue()
    {
        long current = System.currentTimeMillis();
        return (sum + lastValue * (current - this.lastTime)) / (current - firstTime);
    }

    public void doCompute()
    {
        long current = System.currentTimeMillis();
        this.sum += this.lastValue * (current - this.lastTime);
        this.lastValue = getBase().nextValue();
        this.lastTime = current;
    }

}
