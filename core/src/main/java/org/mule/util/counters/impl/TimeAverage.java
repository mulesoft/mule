/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    @Override
    public double nextValue()
    {
        long current = System.currentTimeMillis();
        return (sum + lastValue * (current - this.lastTime)) / (current - firstTime);
    }

    @Override
    public void doCompute()
    {
        long current = System.currentTimeMillis();
        this.sum += this.lastValue * (current - this.lastTime);
        this.lastValue = getBase().nextValue();
        this.lastTime = current;
    }
}
