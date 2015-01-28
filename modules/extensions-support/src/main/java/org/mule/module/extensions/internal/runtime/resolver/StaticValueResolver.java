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
 * A {@link ValueResolver} which always returns the same
 * constant value.
 *
 * @since 3.7.0
 */
public class StaticValueResolver<T> implements ValueResolver<T>
{

    private final T value;

    public StaticValueResolver(T value)
    {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T resolve(MuleEvent event) throws MuleException
    {
        return value;
    }

    /**
     * @return {@value false}
     */
    @Override
    public boolean isDynamic()
    {
        return false;
    }
}
