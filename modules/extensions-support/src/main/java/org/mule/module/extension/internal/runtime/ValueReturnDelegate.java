/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.OperationContext;

/**
 * Simple implementation of {@link ReturnDelegate} which simply returns
 * the provided value as is.
 *
 * This class is intended to be used as a singleton, use the
 * {@link #INSTANCE} attribute to access the instance
 *
 * @since 3.7.0
 */
final class ValueReturnDelegate implements ReturnDelegate
{
    static final ReturnDelegate INSTANCE = new ValueReturnDelegate();

    private ValueReturnDelegate()
    {
    }

    /**
     * {@inheritDoc}
     * @return {@code value}
     */
    @Override
    public Object asReturnValue(Object value, OperationContext operationContext)
    {
        return value;
    }
}
