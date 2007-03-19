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
