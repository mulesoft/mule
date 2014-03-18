/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

/**
 * <p>
 * Utility class to validate Preconditions
 * </p>
 */
public class Preconditions
{

    /**
     * @param condition Condition that the argument must satisfy
     * @param message   The Message of the exception in case the condition is invalid
     */
    public static void checkArgument(boolean condition, String message)
    {
        if (!condition)
        {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @param condition Condition that must be satisfied
     * @param message   The Message of the exception in case the condition is invalid
     */
    public static void checkState(boolean condition, String message)
    {
        if (!condition)
        {
            throw new IllegalStateException(message);
        }
    }
}
