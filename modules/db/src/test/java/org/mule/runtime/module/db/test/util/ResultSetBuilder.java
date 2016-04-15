/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ResultSet} instances for testing purposes
 */
public class ResultSetBuilder
{

    private final List<ColumnMetadata> columns;
    private final List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
    private final Statement statement;

    public ResultSetBuilder(List<ColumnMetadata> columns)
    {
        this(columns, null);
    }

    public ResultSetBuilder(List<ColumnMetadata> columns, Statement statement)
    {
        this.columns = columns;
        this.statement = statement;
    }

    public ResultSetBuilder with(Map<String, Object> record)
    {
        records.add(record);

        return this;
    }

    public ResultSetBuilder with(List<Map<String, Object>> records)
    {
        this.records.addAll(records);

        return this;
    }

    public ResultSet build()
    {
        return new InMemoryResultSet(columns, records, statement);
    }
}
