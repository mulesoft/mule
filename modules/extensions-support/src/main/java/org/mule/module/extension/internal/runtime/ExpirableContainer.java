/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.Expirable;

import java.util.Map;

/**
 * An object which is capable of providing instances of objects that have expired and should be disposed.
 * <p/>
 * Notice that this contract does not specify that those objects should be of any given type, or
 * even implement the {@link Expirable} interface. That's because:
 * <ul>
 * <li>The {@link Expirable} contract is not at all useful once you have already determined that the instance
 * can be expired</li>
 * <li>The returned object is not necessarily the one implementing {@link Expirable}. Often you'll have some
 * wrapper implementing that interface so that you can handle expiration on instances that should not know
 * about that concern</li>
 * </ul>
 *
 * @param <T> the generic type of the objects to be returned
 * @since 4.0
 */
public interface ExpirableContainer<T>
{

    /**
     * Returns a {@link Map} which values are instances which can be expired and the keys are they names
     *
     * @return a {@link Map}
     */
    Map<String, T> getExpired();

}
