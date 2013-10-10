/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.Counter;
import org.mule.util.counters.CounterFactory.Type;

public abstract class AggregateCounter extends AbstractCounter
{

    private final Counter base;

    public AggregateCounter(String name, Type type, AbstractCounter base)
    {
        super(name, type);
        this.base = base;
        base.addAggregate(this);
    }

    public double increment()
    {
        throw new UnsupportedOperationException();
    }

    public double incrementBy(double value)
    {
        throw new UnsupportedOperationException();
    }

    public double decrement()
    {
        throw new UnsupportedOperationException();
    }

    public void setRawValue(double value)
    {
        throw new UnsupportedOperationException();
    }

    public final synchronized void compute()
    {
        this.doCompute();
        this.propagate();
    }

    public Counter getBase()
    {
        return this.base;
    }

    public abstract double nextValue();

    public abstract void doCompute();

}
