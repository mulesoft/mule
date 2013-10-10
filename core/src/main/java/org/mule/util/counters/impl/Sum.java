/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.CounterFactory.Type;

public class Sum extends AggregateCounter
{

    private double value;

    public Sum(String name, AbstractCounter base)
    {
        super(name, Type.SUM, base);
    }

    public double nextValue()
    {
        return this.value;
    }

    public void doCompute()
    {
        this.value += getBase().nextValue();
    }

}
