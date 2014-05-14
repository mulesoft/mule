/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Provides a value which is dependant on a {@link MuleEvent}
 *
 * @since 3.7.0
 */
public interface ValueResolver<T>
{

    /**
     * Resolves a value from the given {@code event}
     *
     * @param event a {@link MuleEvent}
     * @return a resolved value
     * @throws MuleException
     */
    T resolve(MuleEvent event) throws MuleException;

    /**
     * returns {@code false} if subsequent invocations to
     * {@link #resolve(MuleEvent)} will return the same value.
     * Notice that if it returns {@code true}, then it might return different
     * values per invocation but that's not guaranteed.
     *
     * @return whether this resolve is dynamic
     */
    boolean isDynamic();

}
