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

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class Average extends AggregateCounter
{

    private double sum = 0;
    private long times = 0;

    public Average(String name, AbstractCounter base)
    {
        super(name, Type.AVERAGE, base);
    }

    public double nextValue()
    {
        return (times > 0) ? sum / times : 0;
    }

    public void doCompute()
    {
        this.sum += getBase().nextValue();
        this.times++;
    }
}
