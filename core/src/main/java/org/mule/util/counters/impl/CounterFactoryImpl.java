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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CounterFactoryImpl
{

    private static Map counters = new HashMap();
    private static ArrayList publicCounters = new ArrayList();

    /** Do not instanciate. */
    private CounterFactoryImpl ()
    {
        // no-op
    }

    public static Counter getCounter(String name)
    {
        return (Counter) counters.get(name);
    }

    public static Counter createCounter(String name, String first, String second, Type type, boolean visible)
    {
        Counter counter = getCounter(name);
        if (counter != null)
        {
            throw new IllegalStateException();
        }
        else
        {
            counter = internalCreateCounter(name, first, second, type, visible);
        }
        return counter;
    }

    public static Iterator getCounters()
    {
        return publicCounters.iterator();
    }

    protected static AbstractCounter internalCreateCounter(String name,
                                                           String first,
                                                           String second,
                                                           Type type,
                                                           boolean visible)
    {
        AbstractCounter counter = null;
        if (name == null)
        {
            throw new IllegalStateException();
        }
        else if (first == null && second == null)
        {
            if (type == Type.NUMBER)
            {
                counter = new Number(name);
            }
            else
            {
                throw new IllegalStateException();
            }
        }
        else if (first != null && second == null)
        {
            AbstractCounter b = (AbstractCounter) getCounter(first);
            if (b == null)
            {
                throw new IllegalStateException();
            }
            if (type == Type.MIN)
            {
                counter = new Min(name, b);
            }
            else if (type == Type.MAX)
            {
                counter = new Max(name, b);
            }
            else if (type == Type.SUM)
            {
                counter = new Sum(name, b);
            }
            else if (type == Type.AVERAGE)
            {
                counter = new Average(name, b);
            }
            else if (type == Type.TIME_AVERAGE)
            {
                counter = new TimeAverage(name, b);
            }
            else if (type == Type.DELTA)
            {
                counter = new Delta(name, b);
            }
            else if (type == Type.INSTANT_RATE)
            {
                counter = new InstantRate(name, b);
            }
            else if (type == Type.RATE_PER_SECOND || type == Type.RATE_PER_MINUTE
                     || type == Type.RATE_PER_HOUR)
            {
                counter = new RatePerUnit(name, null, type, b);
            }
            else
            {
                throw new IllegalStateException();
            }
        }
        else if (first != null && second != null)
        {
            AbstractCounter b = (AbstractCounter) getCounter(first);
            if (b == null)
            {
                throw new IllegalStateException();
            }
            if (type == Type.RATE_PER_SECOND || type == Type.RATE_PER_MINUTE || type == Type.RATE_PER_HOUR)
            {
                counter = new RatePerUnit(name, second, type, b);
            }
            else if (type == Type.PLUS || type == Type.MINUS || type == Type.MULTIPLY || type == Type.DIVIDE)
            {
                AbstractCounter b2 = (AbstractCounter) getCounter(second);
                if (b2 == null)
                {
                    throw new IllegalStateException();
                }
                counter = new Operator(name, b, b2, type);
            }
            else
            {
                throw new IllegalStateException();
            }
        }
        else
        {
            throw new IllegalStateException();
        }
        counters.put(name, counter);
        if (visible)
        {
            publicCounters.add(counter);
        }
        return counter;
    }
}
