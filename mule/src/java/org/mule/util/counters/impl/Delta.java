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
        if (first == Double.NaN || second == Double.NaN) {
            return Double.NaN;
        } else {
            return second - first > 0.0 ? second - first : 0.0;
        }
    }

    public void doCompute()
    {
        first = second;
        second = getBase().nextValue();
    }

}
