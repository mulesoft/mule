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

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class Operator extends AggregateCounter
{

    private Counter base2;
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
        Type type = getType();
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
            return val2 == 0.0 ? (val1 >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY) : val1
                                                                                                     / val2;
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    public void doCompute()
    {
        this.val1 = getBase().nextValue();
        this.val2 = base2.nextValue();
    }
}
