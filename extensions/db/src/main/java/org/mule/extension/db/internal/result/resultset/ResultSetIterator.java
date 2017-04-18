/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.resultset;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.db.internal.result.row.RowHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;


/**
 * Iterates a {@link ResultSet} to provide rows streaming
 */
public class ResultSetIterator implements Iterator<Map<String, Object>> {

  protected static final Logger LOGGER = getLogger(ResultSetIterator.class);

  private final ResultSet resultSet;
  private final RowHandler rowHandler;
  private Boolean cachedNext = null;

  public ResultSetIterator(ResultSet resultSet, RowHandler rowHandler) {
    this.resultSet = resultSet;
    this.rowHandler = rowHandler;
  }

  @Override
  public boolean hasNext() {
    boolean result = false;
    if (cachedNext == null) {
      try {
        cachedNext = resultSet.next();
        result = cachedNext;
      } catch (SQLException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to determine if there are more records", e);
        }
      }
    } else {
      result = cachedNext;
    }

    return result;
  }


  @Override
  public Map<String, Object> next() {
    try {
      if (cachedNext == null) {
        resultSet.next();
      } else {
        cachedNext = null;
      }

      return rowHandler.process(resultSet);
    } catch (SQLException e) {
      LOGGER.warn("Unable to obtain next row", e);
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
