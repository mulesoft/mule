/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

/**
 * Represents an update count after a {@link java.sql.Statement} execution
 */
public class UpdateCountResult implements StatementResult
{

    private final String name;
    private final int count;

    public UpdateCountResult(String name, int count)
    {
        this.name = name;
        this.count = count;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object getResult()
    {
        return count;
    }
}
