/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.Counter;
import org.mule.util.counters.CounterFactory.Type;

public class Operator extends AggregateCounter
{

    private final Counter base2;
    private double val1;
    private double val2;

    public Operator(String name, AbstractCounter base, AbstractCounter base2, Type type)
    {
        super(name, type, base);
        this.base2 = base2;
        base2.addAggregate(this);
    }

    public double nextValue()
    {
        Type type = this.getType();

        if (type == Type.PLUS)
        {
            return val1 + val2;
        }
        else if (type == Type.MINUS)
        {
            return val1 - val2;
        }
        else if (type == Type.MULTIPLY)
        {
            return val1 * val2;
        }
        else if (type == Type.DIVIDE)
        {
            return val2 == 0.0
                            ? (val1 >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY)
                            : (val1 / val2);
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    public void doCompute()
    {
        this.val1 = this.getBase().nextValue();
        this.val2 = base2.nextValue();
    }

}
