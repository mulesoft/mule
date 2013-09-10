/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

/**
 * <p>
 * {@link Predicate} class to match objects in a query.
 * </p>
 *
 * @param <T> The type of the object to match
 * @since 3.5.0
 */
public interface Predicate<T>
{
    /**
     * @param t The Object to evaluate
     * @return true if the object matches the Predicate
     */
    boolean evaluate(T t);
}
