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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AbstractCounter implements Counter
{

    private Type type;
    private String name;
    private ArrayList aggregates = null;

    public AbstractCounter(String name, Type type)
    {
        this.name = name;
        this.type = type;
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
        if (this.aggregates == null) {
            this.aggregates = new ArrayList();
        }
        this.aggregates.add(counter);
    }

    protected void propagate()
    {
        if (this.aggregates != null) {
            Iterator it = this.aggregates.iterator();
            while (it.hasNext()) {
                AggregateCounter agg = (AggregateCounter) it.next();
                agg.compute();
            }
        }
    }

}
