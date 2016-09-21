/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a data type that was resolved for a database instance
 */
public class ResolvedDbType extends AbstractDbType {

  public ResolvedDbType(int id, String name) {
    super(id, name);
  }

  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException {
    if (value == null) {
      statement.setNull(index, id);
    } else {
      if (value instanceof InputStream) {
        try {
          value = toByteArray((InputStream) value);
        } catch (IOException e) {
          throw new MuleRuntimeException(createStaticMessage("Could not consume inputStream in parameter of index " + index), e);
        }
      }
      statement.setObject(index, value, id);
    }
  }

  @Override
  public Object getParameterValue(CallableStatement statement, int index) throws SQLException {
    return statement.getObject(index);
  }
}
