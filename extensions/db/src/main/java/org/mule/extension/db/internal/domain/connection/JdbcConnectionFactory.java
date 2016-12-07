/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.runtime.api.connection.ConnectionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * A factor for JDBC {@link Connection}s
 *
 * @since 4.0
 */
public class JdbcConnectionFactory {

  /**
   * Creates a new JDBC {@link Connection}
   * 
   * @param dataSource the {@link DataSource} from which the connection comes from
   * @param customDataTypes user defined data types
   * @return a {@link Connection}
   * @throws ConnectionException if the connection could not be established
   */
  public Connection createConnection(DataSource dataSource, List<DbType> customDataTypes) throws ConnectionException {
    Connection connection;
    try {
      connection = dataSource.getConnection();
    } catch (SQLException e) {
      throw new ConnectionCreationException("Could not obtain connection from data source", e);
    }

    if (connection == null) {
      throw new ConnectionCreationException("Unable to create connection to the provided dataSource: " + dataSource);
    }

    Map<String, Class<?>> typeMapping = createTypeMapping(customDataTypes);

    if (typeMapping != null && !typeMapping.isEmpty()) {
      try {
        connection.setTypeMap(typeMapping);
      } catch (SQLException e) {
        throw new ConnectionCreationException("Could not set custom data types on connection", e);
      }
    }

    return connection;
  }

  private Map<String, Class<?>> createTypeMapping(List<DbType> customDataTypes) {
    final Map<String, Class<?>> typeMapping = new HashMap<>();

    customDataTypes.stream()
        .filter(dbType -> dbType instanceof MappedStructResolvedDbType)
        .forEach(dbType -> {
          final MappedStructResolvedDbType structDbType = (MappedStructResolvedDbType) dbType;
          if (structDbType.getMappedClass() != null) {
            typeMapping.put(structDbType.getName(), structDbType.getMappedClass());
          }
        });

    return typeMapping;
  }


}
