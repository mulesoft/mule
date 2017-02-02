/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.lang.String.format;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;

/**
 * Defines a structured data type for {@link Array}
 */
public class ArrayResolvedDbType extends AbstractStructuredDbType {

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
    if (!(value instanceof Array)) {
      Connection connection = statement.getConnection();
      if (value instanceof Object[]) {
        value = connection.createArrayOf(name, (Object[]) value);
      } else if (value instanceof List) {
        value = connection.createArrayOf(name, ((List) value).toArray());
      } else {
        throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
      }
    }

    statement.setArray(index, (Array) value);
  }

  @Override
  public Object getParameterValue(CallableStatement statement, int index) throws SQLException {
    return statement.getArray(index);
  }

  /**
   * Creates error message for the case when a given class is not supported
   *
   * @param value value that was attempted to be converted
   * @return the error message for the provided value's class
   */
  protected static String createUnsupportedTypeErrorMessage(Object value) {
    return format("Cannot create a %s from a value of type %s", Struct.class.getName(), value.getClass());
  }
}
