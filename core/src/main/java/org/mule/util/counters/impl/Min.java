/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class Min extends AggregateCounter
{
    private double min = Double.MAX_VALUE;

    public Min(String name, AbstractCounter base)
    {
        super(name, Type.MIN, base);
    }

    @Override
    public double nextValue()
    {
        return min;
    }

    @Override
    public void doCompute()
    {
        double next = getBase().nextValue();

        if (Double.isNaN(min) || (next < min))
        {
            min = next;
        }
    }
}
