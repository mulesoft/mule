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

public class Number extends AbstractCounter
{

    private double value = 0.0;

    public Number(String name)
    {
        super(name, Type.NUMBER);
    }

    public synchronized double increment()
    {
        this.value++;
        propagate();
        return this.value;
    }

    public synchronized double incrementBy(double value)
    {
        this.value += value;
        propagate();
        return this.value;
    }

    public synchronized double decrement()
    {
        this.value--;
        propagate();
        return this.value;
    }

    public synchronized void setRawValue(double value)
    {
        this.value = value;
        propagate();
    }

    public synchronized double nextValue()
    {
        return this.value;
    }

}
