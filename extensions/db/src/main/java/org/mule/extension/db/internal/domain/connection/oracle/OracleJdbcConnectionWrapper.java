/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;

public class OracleJdbcConnectionWrapper extends AbstractJdbcConnectionWrapper {

  private Method createArrayMethod;
  private boolean initialized;

  public OracleJdbcConnectionWrapper(Connection delegate) {
    super(delegate);
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    if (getCreateArrayOfMethod(delegate) == null) {
      return super.createArrayOf(typeName, elements);
    } else {
      try {
        return (Array) getCreateArrayOfMethod(delegate).invoke(delegate, typeName, elements);
      } catch (Exception e) {
        throw new SQLException("Error creating ARRAY", e);
      }
    }
  }

  private Method getCreateArrayOfMethod(Connection delegate) {
    if (createArrayMethod == null && !initialized) {
      synchronized (this) {
        if (createArrayMethod == null && !initialized) {
          try {
            createArrayMethod = delegate.getClass().getMethod("createARRAY", String.class, Object.class);
            createArrayMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            // Ignore, will use the standard method
          }

          initialized = true;
        }
      }
    }

    return createArrayMethod;
  }
}
