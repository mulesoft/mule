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

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.LinkedList;

import org.mule.util.counters.CounterFactory.Type;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class RatePerUnit extends AggregateCounter
{

    private static class Sample
    {
        public Sample(double value, long time)
        {
            this.value = value;
            this.time = time;
        }

        private double value;
        private long time;

        /**
         * @return the time of the sample
         */
        public long getTime()
        {
            return time;
        }

        /**
         * @return the value of the sample
         */
        public double getValue()
        {
            return value;
        }

    }

    private LinkedList samples;
    private long unit;
    private long length;
    private long baseTime;

    public RatePerUnit(String name, String p, Type type, AbstractCounter base)
    {
        super(name, type, base);
        if (type == Type.RATE_PER_SECOND) {
            unit = 1000;
        } else if (type == Type.RATE_PER_MINUTE) {
            unit = 60 * 1000;
        } else if (type == Type.RATE_PER_HOUR) {
            unit = 60 * 60 * 1000;
        } else {
            throw new InvalidParameterException();
        }
        try {
            length = Long.parseLong(p);
        } catch (Exception e) {
        }
        if (length <= 0) {
            length = 128;
        }
        samples = new LinkedList();
        this.baseTime = System.currentTimeMillis();
    }

    public double nextValue()
    {
        if (samples.isEmpty()) {
            return 0.0;
        } else {
            double total = 0.0;
            long current = getTime();
            Iterator it = samples.iterator();
            Sample sample = null;
            while (it.hasNext()) {
                sample = (Sample) it.next();
                if (current - sample.time > length) {
                    break;
                }
                total += sample.value;
            }
            return total / (1 + current - sample.time);
        }
    }

    public void doCompute()
    {
        Sample l = samples.isEmpty() ? null : (Sample) samples.getFirst();
        long t = getTime();
        if (l == null || t > l.time) {
            Sample s = new Sample(getBase().nextValue(), t);
            samples.addFirst(s);
        } else {
            l.value += getBase().nextValue();
        }
        while (samples.size() > length) {
            samples.removeLast();
        }
    }

    protected long getTime()
    {
        return (System.currentTimeMillis() - baseTime) / unit;
    }

}
