/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.logger.BulkQueryLogger;
import org.mule.module.db.internal.domain.logger.PreparedBulkQueryLogger;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.statement.StatementFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Executes bulk queries
 */
public class BulkUpdateExecutor extends AbstractExecutor implements BulkExecutor
{

    public BulkUpdateExecutor(StatementFactory statementFactory)
    {
        super(statementFactory);
    }

    @Override
    public Object execute(DbConnection connection, BulkQuery bulkQuery) throws SQLException
    {
        Statement statement = statementFactory.create(connection, bulkQuery.getQueryTemplates().get(0));

        try
        {
            BulkQueryLogger queryLogger = queryLoggerFactory.createBulkQueryLogger(logger);

            for (QueryTemplate queryTemplate : bulkQuery.getQueryTemplates())
            {
                String sql = queryTemplate.getSqlText();

                statement.addBatch(sql);
                queryLogger.addQuery(sql);
            }

            queryLogger.logQuery();

            return statement.executeBatch();
        }
        finally
        {
            statement.clearBatch();
            statement.close();
        }
    }

    @Override
    public Object execute(DbConnection connection, Query query, List<List<QueryParamValue>> paramValues) throws SQLException
    {
        Statement statement = statementFactory.create(connection, query.getQueryTemplate());

        if (!(statement instanceof PreparedStatement))
        {
            throw new IllegalArgumentException("Bulk update must be executed on a prepared statement");
        }

        PreparedStatement preparedStatement = (PreparedStatement) statement;
        PreparedBulkQueryLogger queryLogger = queryLoggerFactory.createBulkQueryLogger(logger, query.getQueryTemplate(), paramValues.size());
        try
        {
            for (List<QueryParamValue> params : paramValues)
            {
                doProcessParameters(preparedStatement, query.getQueryTemplate(), params, queryLogger);
                preparedStatement.addBatch();
                queryLogger.addParameterSet();
            }

            queryLogger.logQuery();

            return preparedStatement.executeBatch();
        }
        finally
        {
            preparedStatement.clearParameters();
            statement.close();
        }
    }
}
