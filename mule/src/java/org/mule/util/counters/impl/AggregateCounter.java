/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.util.counters.impl;

import org.mule.util.counters.Counter;
import org.mule.util.counters.CounterFactory.Type;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AggregateCounter extends AbstractCounter
{

    private Counter base;

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

    public void compute()
    {
        doCompute();
        propagate();
    }

    public Counter getBase()
    {
        return this.base;
    }

    public abstract double nextValue();

    public abstract void doCompute();

}
