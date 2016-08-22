/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.oracle.OracleXmlType;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OracleDbConnection extends DefaultDbConnection {

  private static final int CURSOR_TYPE_ID = -10;
  private static final String CURSOR_TYPE_NAME = "CURSOR";

  public OracleDbConnection(Connection jdbcConnection) {
    super(jdbcConnection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    List<DbType> dbTypes = new ArrayList<>();
    dbTypes.add(new ResolvedDbType(CURSOR_TYPE_ID, CURSOR_TYPE_NAME));
    dbTypes.add(new OracleXmlType());

    return dbTypes;
  }
}
