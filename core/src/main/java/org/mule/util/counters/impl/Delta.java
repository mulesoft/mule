/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class Delta extends AggregateCounter
{

    private double first = 0.0;
    private double second = 0.0;

    public Delta(String name, AbstractCounter base)
    {
        super(name, Type.DELTA, base);
    }

    public double nextValue()
    {
        if (Double.isNaN(first) || Double.isNaN(second))
        {
            return Double.NaN;
        }
        else
        {
            return second - first > 0.0 ? second - first : 0.0;
        }
    }

    public void doCompute()
    {
        first = second;
        second = this.getBase().nextValue();
    }

}
