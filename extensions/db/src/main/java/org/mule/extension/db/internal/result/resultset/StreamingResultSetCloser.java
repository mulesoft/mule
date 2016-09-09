/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.resultset;

import org.mule.extension.db.internal.domain.connection.DbConnection;

import java.sql.ResultSet;

/**
 * Closes a {@link ResultSet} that was processed in streaming mode
 */
public interface StreamingResultSetCloser {

  /**
   * Closes the given {@code resultset}. It also must invoke {@link DbConnection#endStreaming()} and
   * {@link DbConnection#release()} even if the operation fails
   *
   * @param connection the connection to the DB
   * @param resultSet the {@link ResultSet}
   */
  void close(DbConnection connection, ResultSet resultSet);
}
