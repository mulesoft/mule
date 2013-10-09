/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.Counter;
import org.mule.util.counters.CounterFactory.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractCounter implements Counter
{

    private final Type type;
    private final String name;
    private final List aggregates;

    public AbstractCounter(String name, Type type)
    {
        super();
        this.name = name;
        this.type = type;
        this.aggregates = Collections.synchronizedList(new ArrayList());
    }

    public Type getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }

    public abstract double increment();

    public abstract double incrementBy(double value);

    public abstract double decrement();

    public abstract void setRawValue(double value);

    public abstract double nextValue();

    protected void addAggregate(AggregateCounter counter)
    {
        this.aggregates.add(counter);
    }

    protected void propagate()
    {
        Iterator it = this.aggregates.iterator();
        while (it.hasNext())
        {
            AggregateCounter agg = (AggregateCounter) it.next();
            agg.compute();
        }
    }

}
