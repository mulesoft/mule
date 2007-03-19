/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
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
