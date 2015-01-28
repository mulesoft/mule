/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

/**
 * Utility class that holds a given value, allowing to set/retrieve it.
 *
 * @since 3.7.0
 */
public class ValueHolder<T>
{

    private T value = null;

    /**
     * Default constructor. Until the {@link #set(Object)} method
     * is invoked, the held {@link #value} will be {@code null}
     */
    public ValueHolder()
    {
    }

    /**
     * Initialises this holder to the given value
     *
     * @param value
     */
    public ValueHolder(T value)
    {
        this();
        set(value);
    }

    /**
     * Returns the given value
     *
     * @return
     */
    public T get()
    {
        return value;
    }

    /**
     * Updates the held value
     *
     * @param value the new value
     * @return the value that got overridden
     */
    public T set(T value)
    {
        T old = this.value;
        this.value = value;

        return old;
    }
}
