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

import org.mule.util.counters.CounterFactory.Type;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class Min extends AggregateCounter
{

    private double min = Double.MAX_VALUE;

    public Min(String name, AbstractCounter base)
    {
        super(name, Type.MIN, base);
    }

    public double nextValue()
    {
        return min;
    }

    public void doCompute()
    {
        double next = getBase().nextValue();
        if (min == Double.NaN) {
            min = next;
        } else if (next < min) {
            min = next;
        }
    }

}
