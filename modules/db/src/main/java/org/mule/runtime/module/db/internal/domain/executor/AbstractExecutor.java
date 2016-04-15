/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

import org.mule.module.db.internal.domain.logger.DefaultQueryLoggerFactory;
import org.mule.module.db.internal.domain.logger.QueryLoggerFactory;
import org.mule.module.db.internal.domain.logger.SingleQueryLogger;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.OutputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.statement.StatementFactory;
import org.mule.module.db.internal.domain.type.DbType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for query executors
 */
public abstract class AbstractExecutor
{

    protected final Log logger = LogFactory.getLog(this.getClass());
    protected final StatementFactory statementFactory;
    protected QueryLoggerFactory queryLoggerFactory = new DefaultQueryLoggerFactory();

    public AbstractExecutor(StatementFactory statementFactory)
    {
        this.statementFactory = statementFactory;
    }

    protected void doProcessParameters(PreparedStatement statement, QueryTemplate queryTemplate, List<QueryParamValue> paramValues, SingleQueryLogger queryLogger) throws SQLException
    {
        int valueIndex = 0;

        for (int paramIndex = 1, inputParamsSize = queryTemplate.getParams().size(); paramIndex <= inputParamsSize; paramIndex++)
        {
            QueryParam queryParam = queryTemplate.getParams().get(paramIndex - 1);
            if (queryParam instanceof InputQueryParam)
            {
                QueryParamValue param = paramValues.get(valueIndex);

                queryLogger.addParameter(queryTemplate.getInputParams().get(valueIndex), param.getValue());

                processInputParam(statement, paramIndex, param.getValue(), queryParam.getType());
                valueIndex++;
            }

            if (queryParam instanceof OutputQueryParam)
            {
                processOutputParam((CallableStatement) statement, paramIndex, queryParam.getType());
            }
        }
    }

    protected void processInputParam(PreparedStatement statement, int index, Object value, DbType type) throws SQLException
    {
        type.setParameterValue(statement, index, value);
    }

    private void processOutputParam(CallableStatement statement, int index, DbType type) throws SQLException
    {
        type.registerOutParameter(statement, index);
    }

    public void setQueryLoggerFactory(QueryLoggerFactory queryLoggerFactory)
    {
        this.queryLoggerFactory = queryLoggerFactory;
    }
}
