/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public double nextValue()
    {
        return min;
    }

    public void doCompute()
    {
        double next = getBase().nextValue();

        if (Double.isNaN(min) || (next < min))
        {
            min = next;
        }
    }

}
