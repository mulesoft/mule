/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
