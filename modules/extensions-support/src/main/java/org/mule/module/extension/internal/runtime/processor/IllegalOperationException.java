/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

/**
 * A {@link RuntimeException} which marks that a selected operation
 * is not valid or misconfigured
 *
 * @since 4.0
 */
public class IllegalOperationException extends RuntimeException
{

    public IllegalOperationException(String message)
    {
        super(message);
    }

    public IllegalOperationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
