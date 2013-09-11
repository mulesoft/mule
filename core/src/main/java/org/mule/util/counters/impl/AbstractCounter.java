/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.Counter;
import org.mule.util.counters.CounterFactory.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCounter implements Counter
{
    private final Type type;
    private final String name;
    private final List<AggregateCounter> aggregates;

    public AbstractCounter(String name, Type type)
    {
        super();
        this.name = name;
        this.type = type;
        this.aggregates = Collections.synchronizedList(new ArrayList<AggregateCounter>());
    }

    @Override
    public Type getType()
    {
        return this.type;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public abstract double increment();

    @Override
    public abstract double incrementBy(double value);

    @Override
    public abstract double decrement();

    @Override
    public abstract void setRawValue(double value);

    @Override
    public abstract double nextValue();

    protected void addAggregate(AggregateCounter counter)
    {
        this.aggregates.add(counter);
    }

    protected void propagate()
    {
        for (AggregateCounter counter : this.aggregates)
        {
            counter.compute();
        }
    }
}
