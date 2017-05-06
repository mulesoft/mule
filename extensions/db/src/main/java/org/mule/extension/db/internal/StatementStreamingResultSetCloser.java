/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal;

import org.mule.extension.db.internal.domain.connection.DbConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Closes a {@link ResultSet} once it has been processed
 *
 * @since 4.0
 */
public class StatementStreamingResultSetCloser {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatementStreamingResultSetCloser.class);

  private final DbConnection connection;
  private final Set<ResultSet> resultSets = Collections.newSetFromMap(new ConcurrentHashMap<>());

  public StatementStreamingResultSetCloser(DbConnection connection) {
    this.connection = connection;
  }

  /**
   * Closes all tracked {@link ResultSet}s
   */
  public void closeResultSets() {
    try {
      for (ResultSet resultSet : resultSets) {
        close(resultSet);
      }
    } finally {
      resultSets.clear();
      connection.endStreaming();
      connection.release();
    }
  }

  private void close(ResultSet resultSet) {
    try {
      if (!resultSet.isClosed()) {
        resultSet.close();
      }
    } catch (SQLException e) {
      LOGGER.warn("Error attempting to close resultSet", e);
    }
  }

  /**
   * Adds a resultSet for tracking in order to be able to close it later
   *
   * @param resultSet resultSet to track
   */
  public void trackResultSet(ResultSet resultSet) {
    resultSets.add(resultSet);
  }

  public int getOpenResultSets() {
    return resultSets.size();
  }
}
