/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;

/**
 * A delegate interface to decouple a {@link OperationExecutor}'s return value
 * from the format in which it is to be handed off
 *
 * @since 3.7.0
 */
interface ReturnDelegate
{

    /**
     * Adapts the {@code value} to another format
     *
     * @param value            the value to be returned
     * @param operationContext the {@link OperationContext} on which the operation was executed
     * @return an adapted value
     */
    Object asReturnValue(Object value, OperationContext operationContext);
}
