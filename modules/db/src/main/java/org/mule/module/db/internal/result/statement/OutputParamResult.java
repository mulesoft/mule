/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

/**
 * Represents an output parameter after a {@link java.sql.Statement} execution
 */
public class OutputParamResult implements StatementResult
{

    private final String name;
    private final Object value;

    public OutputParamResult(String name, Object value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object getResult()
    {
        return value;
    }
}
