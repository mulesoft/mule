/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.statement.AbstractStreamingResultSetCloser;

import java.sql.ResultSet;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Closes a {@link ResultSet} once it has been processed
 * 
 * @since 4.0
 */
public class StatementStreamingResultSetCloser extends AbstractStreamingResultSetCloser {

  private final ConcurrentHashMap<DbConnection, Set<ResultSet>> connectionResultSets = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<DbConnection, Object> connectionLocks = new ConcurrentHashMap<>();

  public StatementStreamingResultSetCloser() {
    super();
  }

  public StatementStreamingResultSetCloser(boolean autoCloseConnection) {
    super(autoCloseConnection);
  }

  /**
   * Closes all tracked {@link ResultSet}s for the passed {@code connection}.
   * 
   * @param connection
   */
  public void closeResultSets(DbConnection connection) {
    Object connectionLock = getConnectionLock(connection);

    synchronized (connectionLock) {
      checkValidConnectionLock(connection, connectionLock);

      Set<ResultSet> resultSets = connectionResultSets.get(connection);
      if (resultSets != null) {
        for (ResultSet resultSet : resultSets) {
          super.close(connection, resultSet);
        }
      }

      releaseResources(connection, connectionLock);
    }
  }

  @Override
  public void close(DbConnection connection, ResultSet resultSet) {
    Object connectionLock = getTrackedConnectionLock(connection);

    synchronized (connectionLock) {
      checkValidConnectionLock(connection, connectionLock);

      Set<ResultSet> resultSets = getConnectionResultSets(connection, resultSet);
      try {
        super.close(connection, resultSet);
      } finally {
        if (isEmpty(resultSets)) {
          releaseResources(connection, connectionLock);
        }
      }
    }
  }

  /**
   * Adds a resultSet for tracking in order to be able to close it later
   *
   * @param connection connection that holds the resultSet
   * @param resultSet resultSet to track
   */
  public void trackResultSet(DbConnection connection, ResultSet resultSet) {
    Object connectionLock = getConnectionLock(connection);

    synchronized (connectionLock) {
      Set<ResultSet> resultSets = connectionResultSets.get(connection);

      if (resultSets == null) {
        resultSets = new HashSet<>();
        connectionResultSets.put(connection, resultSets);
      }

      resultSets.add(resultSet);
    }
  }

  protected Object getTrackedConnectionLock(DbConnection connection) {
    Object connectionLock = connectionLocks.get(connection);

    if (connectionLock == null) {
      throw new IllegalStateException("Attempting to close resultSet from non tracked connection");
    }

    return connectionLock;
  }

  protected void releaseResources(DbConnection connection, Object connectionLock) {
    connectionResultSets.remove(connection);
    connectionLocks.remove(connectionLock);

    connection.release();
  }

  protected Set<ResultSet> getConnectionResultSets(DbConnection connection, ResultSet resultSet) {
    Set<ResultSet> resultSets = connectionResultSets.get(connection);

    if (resultSets != null && !resultSets.remove(resultSet)) {
      throw new IllegalStateException("Attempting to close non tracked resultSet");
    }
    return resultSets;
  }

  protected void checkValidConnectionLock(DbConnection connection, Object connectionLock) {
    if (connectionLock != connectionLocks.get(connection)) {
      throw new ConcurrentModificationException("Connection lock modified in another thread");
    }
  }

  protected Object getConnectionLock(DbConnection connection) {
    Object connectionLock = connectionLocks.get(connection);

    if (connectionLock == null) {
      connectionLock = new Object();
      Object oldConnectionLock = connectionLocks.putIfAbsent(connection, connectionLock);
      if (oldConnectionLock != null) {
        connectionLock = oldConnectionLock;
      }
    }

    return connectionLock;
  }
}
