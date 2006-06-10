/* 
 * $Id$
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

import org.mule.util.counters.CounterFactory.Type;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class TimeAverage extends AggregateCounter
{

    private double sum = 0.0;
    private double lastValue = 0.0;
    private long firstTime = System.currentTimeMillis();
    private long lastTime = firstTime;

    public TimeAverage(String name, AbstractCounter base)
    {
        super(name, Type.AVERAGE, base);
    }

    public double nextValue()
    {
        long current = System.currentTimeMillis();
        return (sum + lastValue * (current - this.lastTime)) / (current - firstTime);
    }

    public void doCompute()
    {
        long current = System.currentTimeMillis();
        this.sum += this.lastValue * (current - this.lastTime);
        this.lastValue = getBase().nextValue();
        this.lastTime = current;
    }
}
