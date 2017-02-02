/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.statement;

import org.mule.extension.db.internal.domain.autogeneratedkey.AutoGenerateKeysStrategy;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.OutputQueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetProcessingException;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterates across all the {@link SingleStatementResult} returned by a {@link Statement} execution.
 */
public class StatementResultIterator implements Iterator<SingleStatementResult> {

  public static final int NO_UPDATE_COUNT = -1;
  private static final Logger LOGGER = LoggerFactory.getLogger(StatementResultIterator.class);
  private final Statement statement;
  private final QueryTemplate queryTemplate;
  private final AutoGenerateKeysStrategy autoGenerateKeysStrategy;
  private final DbConnection connection;
  private final ResultSetHandler resultSetHandler;
  private final int outputParamsSize;

  private Boolean cachedResult = null;
  private ResultSet resultSet;
  private int updateCount;
  private int currentOutputParam;
  private int updateCountIndex = 1;
  private int resultSetIndex = 1;
  private boolean isFirstInvocation = true;
  private ResultSet generatedKeys;
  private boolean processedGeneratedKeyResultSet;
  private boolean hasProcessedResultSet;

  public StatementResultIterator(DbConnection connection, Statement statement, QueryTemplate queryTemplate,
                                 AutoGenerateKeysStrategy autoGenerateKeysStrategy, ResultSetHandler resultSetHandler) {
    this.statement = statement;
    this.queryTemplate = queryTemplate;
    this.autoGenerateKeysStrategy = autoGenerateKeysStrategy;
    this.connection = connection;
    this.resultSetHandler = resultSetHandler;

    outputParamsSize = queryTemplate.getOutputParams().size();
    currentOutputParam = 0;
  }

  @Override
  public boolean hasNext() {
    if (cachedResult != null) {
      return cachedResult;
    }

    try {
      if (!isFirstInvocation) {
        if (!processedGeneratedKeyResultSet) {
          if (retrieveAutoGeneratedKeys()) {
            generatedKeys = statement.getGeneratedKeys();
            processedGeneratedKeyResultSet = true;
          } else {
            processedGeneratedKeyResultSet = true;
          }
        }

        if (generatedKeys == null) {
          moveToNextResult();
        }
      } else {
        isFirstInvocation = false;
      }

      if (generatedKeys != null) {
        cachedResult = true;
        return true;
      }

      resultSet = statement.getResultSet();

      if (resultSet != null) {
        cachedResult = true;
        return true;
      }

      updateCount = statement.getUpdateCount();
      if (updateCount != NO_UPDATE_COUNT) {
        cachedResult = true;
        return true;
      }

      cachedResult = currentOutputParam < outputParamsSize;

      return cachedResult;
    } catch (SQLException e) {
      LOGGER.warn("Unable to determine if there are more statement results", e);
      return false;
    }
  }

  protected boolean retrieveAutoGeneratedKeys() {
    return autoGenerateKeysStrategy.returnsAutoGenerateKeys();
  }

  @Override
  public SingleStatementResult next() {
    if (cachedResult == null) {
      hasNext();
    }

    cachedResult = null;

    SingleStatementResult result;
    if (resultSet != null) {
      result = processResultSet();
      hasProcessedResultSet = true;
      resultSet = null;
    } else if (updateCount != NO_UPDATE_COUNT) {
      result = doProcessUpdateCount("updateCount" + updateCountIndex++, updateCount);
      updateCount = NO_UPDATE_COUNT;
    } else if (generatedKeys != null) {
      result = processGeneratedKeys();
      generatedKeys = null;
    } else if (currentOutputParam < outputParamsSize) {
      result = processOutputParam();
      currentOutputParam++;
    } else {
      throw new NoSuchElementException();
    }

    return result;
  }

  private SingleStatementResult processGeneratedKeys() {
    SingleStatementResult generatedKeysResult;

    try {
      generatedKeysResult = doProcessResultSet("generatedKeys", generatedKeys);
    } catch (SQLException e) {
      LOGGER.warn("Unable to obtain auto generated keys", e);
      throw new AutoGeneratedKeysProcessingException(e);
    }

    return generatedKeysResult;
  }

  private void moveToNextResult() throws SQLException {
    if (connection.getJdbcConnection().getMetaData().supportsMultipleOpenResults()) {
      statement.getMoreResults(Statement.KEEP_CURRENT_RESULT);
    } else {
      if (hasProcessedResultSet && resultSetHandler.requiresMultipleOpenedResults()) {
        throw new IllegalStateException("Database does not supports streaming of resultSets on stored procedures");
      } else {
        statement.getMoreResults();
      }
    }
  }

  protected SingleStatementResult processOutputParam() {
    OutputQueryParam outputSqlParam = queryTemplate.getOutputParams().get(currentOutputParam);

    try {
      Object paramValue = outputSqlParam.getType().getParameterValue((CallableStatement) statement, outputSqlParam.getIndex());

      return doProcessOutputParam(outputSqlParam, paramValue);
    } catch (SQLException e) {
      LOGGER.warn("Unable to obtain output parameter", e);
      throw new OutputParamProcessingException(e);
    }
  }

  protected SingleStatementResult doProcessOutputParam(OutputQueryParam outputSqlParam, Object paramValue) throws SQLException {
    if (paramValue instanceof ResultSet) {
      paramValue = resultSetHandler.processResultSet(connection, (ResultSet) paramValue);
    } else if (paramValue instanceof SQLXML) {
      SQLXML sqlxml = (SQLXML) paramValue;

      paramValue = sqlxml.getString();
    }

    return new OutputParamResult(outputSqlParam.getName(), paramValue);
  }

  protected SingleStatementResult doProcessUpdateCount(String name, int value) {
    return new UpdateCountResult(name, value);
  }

  private SingleStatementResult processResultSet() {
    SingleStatementResult result;

    String name = "resultSet" + resultSetIndex++;

    try {
      result = doProcessResultSet(name, resultSet);
    } catch (SQLException e) {
      LOGGER.warn("Unable to obtain next resultSet", e);
      throw new ResultSetProcessingException("Error processing result set: " + name, e);
    }

    return result;
  }

  protected SingleStatementResult doProcessResultSet(String name, ResultSet resultSet) throws SQLException {
    Object handledResultSet = resultSetHandler.processResultSet(connection, resultSet);
    return new ResultSetResult(name, handledResultSet);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
