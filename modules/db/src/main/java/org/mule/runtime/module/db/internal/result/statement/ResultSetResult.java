/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

/**
 * Represents a {@link java.sql.ResultSet} after a {@link java.sql.Statement} execution
 */
public class ResultSetResult implements StatementResult
{

    private final String name;
    private final Object resultSet;

    public ResultSetResult(String name, Object resultSet)
    {
        this.name = name;
        this.resultSet = resultSet;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object getResult()
    {
        return resultSet;
    }
}
