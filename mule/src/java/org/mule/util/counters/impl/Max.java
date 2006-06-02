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
        double next = getBase().nextValue();
        if (max == Double.NaN) {
            max = next;
        } else if (next > max) {
            max = next;
        }
    }

}
