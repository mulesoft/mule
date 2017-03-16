/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.util;

public class ValueEvaluator
{

    private static final String NULL_VALUE = "NULL";

    public static Object checkNullValue(Object value)
    {
        if (NULL_VALUE.equals(value))
        {
            return null;
        }

        return value;
    }

}
