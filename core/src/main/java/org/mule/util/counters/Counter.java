/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.counters;

import org.mule.api.NamedObject;

/**
 * This interface is the interface implemented for all counter types. A Counter can
 * represent a real counter or a virtual counter that will be computed using one or
 * more other counters.<br/>
 * <h3>Real counters</h3>
 * are counters which represent real values. The user will call methods of such
 * counters to modify the associated value of the counter.
 * <h3>Computed counters</h3>
 * are computed using one or more associated counters. Such counters represent
 * operations computed on associated counters. Usually, these counters will never be
 * used directly, but will only used to retrieve the computed values.
 */
public interface Counter extends NamedObject
{

    /**
     * Accessor for the counter type.
     * 
     * @return the type of the counter
     */
    CounterFactory.Type getType();

    /**
     * Increment the counter's value by 1.0.
     * 
     * @return the new value of the counter
     */
    double increment();

    /**
     * Increment the counter's value by the specified amount.
     * 
     * @param value the amount to increment the counter by
     * @return the new value of the counter
     */
    double incrementBy(double value);

    /**
     * Decrement the counter's value by 1.0.
     * 
     * @return the new value of the counter
     */
    double decrement();

    /**
     * Set the counter's value to a new value.
     * 
     * @param value the new value of the counter
     */
    void setRawValue(double value);

    /**
     * Compute and return the value of the counter.
     * 
     * @return the value of the counter
     */
    double nextValue();

}
