/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.domain.executor;

import org.mule.runtime.module.db.internal.domain.autogeneratedkey.AutoGeneratedKeyStrategy;
import org.mule.runtime.module.db.internal.domain.connection.DbConnection;
import org.mule.runtime.module.db.internal.domain.logger.SingleQueryLogger;
import org.mule.runtime.module.db.internal.domain.query.Query;
import org.mule.runtime.module.db.internal.domain.statement.StatementFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for executors that execute a single query
 */
public abstract class AbstractSingleQueryExecutor extends AbstractExecutor implements QueryExecutor
{

    public AbstractSingleQueryExecutor(StatementFactory statementFactory)
    {
        super(statementFactory);
    }

    @Override
    public Object execute(DbConnection connection, Query query) throws SQLException
    {
        Statement statement = statementFactory.create(connection, query.getQueryTemplate());

        prepareQuery(statement, query);

        return doExecuteQuery(connection, statement, query);
    }

    @Override
    public Object execute(DbConnection connection, Query query, AutoGeneratedKeyStrategy autoGeneratedKeyStrategy) throws SQLException
    {
        Statement statement = statementFactory.create(connection, query.getQueryTemplate(), autoGeneratedKeyStrategy);

        prepareQuery(statement, query);

        return doExecuteQuery(connection, statement, query, autoGeneratedKeyStrategy);
    }

    protected abstract Object doExecuteQuery(DbConnection connection, Statement statement, Query query) throws SQLException;

    protected abstract Object doExecuteQuery(DbConnection dbConnection, Statement statement, Query query, AutoGeneratedKeyStrategy autoGeneratedKeyStrategy) throws SQLException;

    protected void prepareQuery(Statement statement, Query query) throws SQLException
    {
        SingleQueryLogger queryLogger = queryLoggerFactory.createQueryLogger(logger, query.getQueryTemplate());

        if (statement instanceof PreparedStatement)
        {
            doProcessParameters((PreparedStatement) statement, query.getQueryTemplate(), query.getParamValues(), queryLogger);
        }

        queryLogger.logQuery();
    }
}
