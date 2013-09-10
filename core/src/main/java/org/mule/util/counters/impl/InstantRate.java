/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    @Override
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

    @Override
    public void doCompute()
    {
        firstTime = lastTime;
        lastTime = System.currentTimeMillis();
        value = getBase().nextValue();
    }
}
