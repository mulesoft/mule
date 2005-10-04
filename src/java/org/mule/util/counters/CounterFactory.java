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
package org.mule.util.counters;

import org.mule.util.counters.impl.CounterFactoryImpl;

import java.util.Iterator;

/**
 * This class is the Counter's factory. It is the main entry point for
 * operations on counters. The user can:
 * <ul>
 * <li>retrieve a counter by its name</li>
 * <li>create a counter</li>
 * <li>retrieve a list of public counters</li>
 * </ul>
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class CounterFactory
{

    /**
     * Enum class for all different types of counters. The type of a counter is
     * defined on creation and can be retrieved on each counter.
     * 
     * @author gnt
     */
    public static final class Type
    {

        /** A basic counter representing a double value */
        public static final Type NUMBER = new Type("Number");
        /** Counter representing the sum of two counters */
        public static final Type SUM = new Type("Sum");
        /** Counter representing the minimum value of a counter */
        public static final Type MIN = new Type("Min");
        /** Counter representing the maximum value of a counter */
        public static final Type MAX = new Type("Max");
        /** Counter representing the average value of a counter */
        public static final Type AVERAGE = new Type("Average");
        /** Counter representing the time average value of a counter */
        public static final Type TIME_AVERAGE = new Type("TimeAverage");
        /** Counter representing the variation of a counter */
        public static final Type DELTA = new Type("Delta");
        /** Counter representing the instant rate of a counter */
        public static final Type INSTANT_RATE = new Type("InstantRate");
        /** Counter representing rate per second of a counter */
        public static final Type RATE_PER_SECOND = new Type("RatePerSecond");
        /** Counter representing rate per minute of a counter */
        public static final Type RATE_PER_MINUTE = new Type("RatePerMinute");
        /** Counter representing rate per hour of a counter */
        public static final Type RATE_PER_HOUR = new Type("RatePerHour");
        /** Counter represening the sum of two other counters */
        public static final Type PLUS = new Type("Plus");
        /** Counter represening the difference of two other counters */
        public static final Type MINUS = new Type("Minus");
        /** Counter represening the multiplication of two other counters */
        public static final Type MULTIPLY = new Type("Multiply");
        /** Counter represening the division of two other counters */
        public static final Type DIVIDE = new Type("Divide");

        /** The string representation of this counter type */
        private String name;

        /**
         * Constructor of the type
         * 
         * @param name the name of the counter type
         */
        protected Type(String name)
        {
            this.name = name;
        }
        
        public String getName()
        {
        	return this.name;
        }
    }

    /**
     * Search the defined counters for a counter of the given name.
     * 
     * @param name the name of the counter to retrieve
     * @return the counter
     */
    public static Counter getCounter(String name)
    {
        return CounterFactoryImpl.getCounter(name);
    }

    /**
     * Create a new public counter of the given type.
     * 
     * @param name the name of the counter to create
     * @param type the type of the counter
     * @return the newly created counter
     */
    public static Counter createCounter(String name, Type type)
    {
        return createCounter(name, null, null, type, true);
    }

    /**
     * Create a new counter of the given type and visibility.
     * 
     * @param name the name of the counter to create
     * @param type the type of the counter
     * @param visible boolean specifying if the counter is public or not
     * @return the newly created counter
     */
    public static Counter createCounter(String name, Type type, boolean visible)
    {
        return createCounter(name, null, null, type, visible);
    }

    /**
     * Create a new public aggregate counter of the given type.
     * 
     * @param name the name of the counter to create
     * @param base the name of the counter to use for computation
     * @param type the type of the counter
     * @return the newly created counter
     */
    public static Counter createCounter(String name, String base, Type type)
    {
        return createCounter(name, base, null, type, true);
    }

    /**
     * Create a new aggregate counter of the given type and visibility.
     * 
     * @param name the name of the counter to create
     * @param base the name of the counter to use for computation
     * @param type the type of the counter
     * @param visible boolean specifying if the counter is public or not
     * @return the newly created counter
     */
    public static Counter createCounter(String name, String base, Type type, boolean visible)
    {
        return createCounter(name, base, null, type, visible);
    }

    /**
     * Create a new public aggregate counter of the given type.
     * 
     * @param name the name of the counter to create
     * @param first the name of the first counter to use for computation
     * @param second the name of the first counter to use for computation
     * @param type the type of the counter
     * @return the newly created counter
     */
    public static Counter createCounter(String name, String first, String second, Type type)
    {
        return createCounter(name, first, second, type, true);
    }

    /**
     * Create a new aggregate counter of the given type and visibility.
     * 
     * @param name the name of the counter to create
     * @param first the name of the first counter to use for computation
     * @param second the name of the first counter to use for computation
     * @param type the type of the counter
     * @param visible boolean specifying if the counter is public or not
     * @return the newly created counter
     */
    public static Counter createCounter(String name, String first, String second, Type type, boolean visible)
    {
        return CounterFactoryImpl.createCounter(name, first, second, type, visible);
    }

    /**
     * Retrieve an iterator giving the list of public defined counters.
     * 
     * @return an iterator to walk throught the list of public defined counters
     */
    public static Iterator getCounters()
    {
        return CounterFactoryImpl.getCounters();
    }

}
