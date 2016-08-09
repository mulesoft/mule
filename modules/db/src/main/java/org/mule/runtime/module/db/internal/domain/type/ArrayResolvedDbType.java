/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.domain.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a structured data type for {@link Array}
 */
public class ArrayResolvedDbType extends ResolvedDbType {

  /**
   * Creates a new instance
   *
   * @param id identifier for the type
   * @param name type name. Non Empty.
   */
  public ArrayResolvedDbType(int id, String name) {
    super(id, name);
  }

  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException {
    statement.setArray(index, (Array) value);
  }

  @Override
  public Object getParameterValue(CallableStatement statement, int index) throws SQLException {
    return statement.getArray(index);
  }
}
