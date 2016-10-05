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
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a Blob data type that was resolved for a database instance.
 *
 * @since 4.0
 */
public class BlobDbType extends ResolvedDbType {

  public BlobDbType(int id, String name) {
    super(id, name);
  }

  /**
   * Sets the parameter accounting for the case in which the {@code value} is
   * an {@link InputStream}, in which case it is consumed into a {@code byte[]} and
   * set.
   * {@inheritDoc}
   */
  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException {
    if (value instanceof byte[]) {
      Blob blob = statement.getConnection().createBlob();
      blob.setBytes(1, (byte[]) value);
      value = blob;
    } else if (value instanceof InputStream) {
      try {
        Blob blob = statement.getConnection().createBlob();
        blob.setBytes(1, toByteArray((InputStream) value));
        value = blob;
      } catch (IOException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not consume inputStream in parameter of index " + index), e);
      }
    }

    super.setParameterValue(statement, index, value);
  }
}
