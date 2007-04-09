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

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.LinkedList;

public class RatePerUnit extends AggregateCounter
{

    private static class Sample
    {
        private double value;
        private long time;

        public Sample(double value, long time)
        {
            this.value = value;
            this.time = time;
        }

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

    private final LinkedList samples;
    private final long unit;
    private final long length;
    private final long baseTime;

    public RatePerUnit(String name, String p, Type type, AbstractCounter base)
    {
        super(name, type, base);

        if (type == Type.RATE_PER_SECOND)
        {
            unit = 1000;
        }
        else if (type == Type.RATE_PER_MINUTE)
        {
            unit = 60 * 1000;
        }
        else if (type == Type.RATE_PER_HOUR)
        {
            unit = 60 * 60 * 1000;
        }
        else
        {
            throw new InvalidParameterException();
        }

        long newLength = 0;

        try
        {
            newLength = Long.parseLong(p);
        }
        catch (Exception e)
        {
            newLength = 0;
        }
        finally
        {
            if (newLength <= 0)
            {
                newLength = 128;
            }

            length = newLength;
        }

        samples = new LinkedList();
        baseTime = System.currentTimeMillis();
    }

    public double nextValue()
    {
        if (samples.isEmpty())
        {
            return 0.0;
        }
        else
        {
            double total = 0.0;
            long current = getTime();
            Iterator it = samples.iterator();
            Sample sample = null;
            while (it.hasNext())
            {
                sample = (Sample) it.next();
                if (current - sample.time > length)
                {
                    break;
                }
                total += sample.value;
            }
            return total / (1 + current - (sample != null ? sample.time : 0));
        }
    }

    public void doCompute()
    {
        Sample l = samples.isEmpty() ? null : (Sample) samples.getFirst();
        long t = getTime();
        if (l == null || t > l.time)
        {
            Sample s = new Sample(this.getBase().nextValue(), t);
            samples.addFirst(s);
        }
        else
        {
            l.value += getBase().nextValue();
        }
        while (samples.size() > length)
        {
            samples.removeLast();
        }
    }

    protected long getTime()
    {
        return (System.currentTimeMillis() - baseTime) / unit;
    }

}
