/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.extension.runtime.OperationContext;

/**
 * A component for resolving the value of an operation's argument
 *
 * @param <T> the type of the argument to be resolved
 * @since 3.7.0
 */
public interface ArgumentResolver<T>
{

    /**
     * Resolves an argument's value from the given {@code operationContext}
     *
     * @param operationContext an {@link OperationContext}
     * @return a value
     */
    T resolve(OperationContext operationContext);
}
