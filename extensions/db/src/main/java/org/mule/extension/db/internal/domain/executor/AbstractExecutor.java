/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import static java.lang.String.format;
import org.mule.extension.db.internal.domain.logger.DefaultQueryLoggerFactory;
import org.mule.extension.db.internal.domain.logger.QueryLoggerFactory;
import org.mule.extension.db.internal.domain.logger.SingleQueryLogger;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.param.OutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.statement.StatementFactory;
import org.mule.extension.db.internal.domain.type.DbType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for query executors
 */
public abstract class AbstractExecutor {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractExecutor.class);

  protected final StatementFactory statementFactory;
  protected QueryLoggerFactory queryLoggerFactory = new DefaultQueryLoggerFactory();

  public AbstractExecutor(StatementFactory statementFactory) {
    this.statementFactory = statementFactory;
  }

  protected void doProcessParameters(PreparedStatement statement, QueryTemplate queryTemplate, List<QueryParamValue> paramValues,
                                     SingleQueryLogger queryLogger)
      throws SQLException {
    int valueIndex = 0;

    for (int paramIndex = 1, inputParamsSize = queryTemplate.getParams().size(); paramIndex <= inputParamsSize; paramIndex++) {
      QueryParam queryParam = queryTemplate.getParams().get(paramIndex - 1);
      if (queryParam instanceof InputQueryParam) {
        QueryParamValue param = getParamValue(paramValues, queryParam.getName());

        queryLogger.addParameter(queryTemplate.getInputParams().get(valueIndex), param.getValue());

        processInputParam(statement, paramIndex, param.getValue(), queryParam.getType());
        valueIndex++;
      }

      if (queryParam instanceof OutputQueryParam) {
        processOutputParam((CallableStatement) statement, paramIndex, queryParam.getType());
      }
    }
  }

  protected void processInputParam(PreparedStatement statement, int index, Object value, DbType type) throws SQLException {
    type.setParameterValue(statement, index, value);
  }

  private void processOutputParam(CallableStatement statement, int index, DbType type) throws SQLException {
    type.registerOutParameter(statement, index);
  }

  private QueryParamValue getParamValue(List<QueryParamValue> paramValues, String paramName) {
    return paramValues.stream().filter(p -> p.getName().equals(paramName)).findFirst().orElseThrow(
                                                                                                   () -> new IllegalArgumentException(format("SQL Query references parameter '%s' which was not supplied as an input parameter",
                                                                                                                                             paramName)));
  }
}
