/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class Average extends AggregateCounter
{
    private double sum = 0;
    private long times = 0;

    public Average(String name, AbstractCounter base)
    {
        super(name, Type.AVERAGE, base);
    }

    @Override
    public double nextValue()
    {
        return (times > 0) ? sum / times : 0;
    }

    @Override
    public void doCompute()
    {
        this.sum += getBase().nextValue();
        this.times++;
    }
}
