/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.util;

/**
 * This class groups utility methods to modify DB parameter values.
 */
public class ValueUtils
{

    private static final String NULL_VALUE = "NULL";

    private ValueUtils()
    {
    }

    /**
     *
     * @param value, the value of a DB parameter.
     * @return null if the value is a NULL String, the same value otherwise.
     */
    public static Object convertsNullStringToNull(Object value)
    {
        if (NULL_VALUE.equals(value))
        {
            return null;
        }

        return value;
    }

}
