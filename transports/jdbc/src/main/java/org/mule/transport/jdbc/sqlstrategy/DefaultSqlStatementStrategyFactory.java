/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.sqlstrategy;

public class DefaultSqlStatementStrategyFactory implements SqlStatementStrategyFactory
{
    protected SimpleUpdateSqlStatementStrategy simpleUpdateSQLStrategy;
    protected SelectSqlStatementStrategy selectSQLStrategy;
    protected CallableSqlStatementStrategy callableSQLStrategy;

    public DefaultSqlStatementStrategyFactory()
    {
        simpleUpdateSQLStrategy = new SimpleUpdateSqlStatementStrategy();
        selectSQLStrategy = new SelectSqlStatementStrategy();
        callableSQLStrategy = new CallableSqlStatementStrategy();
    }

    public SqlStatementStrategy create(String sql, Object payload)
        throws Exception
    {
        String sqlLowerCase = sql.toLowerCase();

        if( sqlLowerCase.startsWith("insert") ||
            sqlLowerCase.startsWith("update") ||
            sqlLowerCase.startsWith("delete") ||
            sqlLowerCase.startsWith("merge"))
        {
            return simpleUpdateSQLStrategy;
        }

        if (sqlLowerCase.startsWith("select"))
        {
            return selectSQLStrategy;
        }

        if (sqlLowerCase.startsWith("call"))
        {
            return callableSQLStrategy;
        }

        throw new IllegalArgumentException("No SQL Strategy found for SQL statement: " + sql);
    }

}
