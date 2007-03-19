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

import org.mule.util.counters.CounterFactory.Type;

public class Max extends AggregateCounter
{

    private double max = Double.MIN_VALUE;

    public Max(String name, AbstractCounter base)
    {
        super(name, Type.MAX, base);
    }

    public double nextValue()
    {
        return max;
    }

    public void doCompute()
    {
        double next = this.getBase().nextValue();

        if (Double.isNaN(max) || (next > max))
        {
            max = next;
        }
    }

}
