/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
