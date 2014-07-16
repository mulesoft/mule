/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.query;

/**
 * Contains the value for a query parameter
 */
public class QueryParamValue
{

    private final Object value;
    private final String name;

    public QueryParamValue(String name, Object value)
    {
        this.value = value;
        this.name = name;
    }

    public Object getValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }
}
