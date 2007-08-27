/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
