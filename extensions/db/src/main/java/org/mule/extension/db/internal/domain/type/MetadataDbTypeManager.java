/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static org.mule.extension.db.api.param.JdbcType.ARRAY;
import static org.mule.extension.db.api.param.JdbcType.DISTINCT;
import static org.mule.extension.db.api.param.JdbcType.STRUCT;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.resultset.ResultSetIterator;
import org.mule.extension.db.internal.result.resultset.SingleResultResultSetCloser;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages database types that are discovered using database metadata
 */
public class MetadataDbTypeManager implements DbTypeManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataDbTypeManager.class);
  static final String METADATA_TYPE_ID_COLUMN = "DATA_TYPE";
  static final String METADATA_TYPE_NAME_COLUMN = "TYPE_NAME";

  private final Map<String, DbType> typesById = new HashMap<>();
  private final Object lock = new Object();
  private boolean initialised;

  protected void registerType(DbType dbType) {
    String typeKey = dbType.getName() + dbType.getId();
    if (typesById.containsKey(typeKey)) {
      LOGGER.warn(String.format("There is already a registered type with ID %s and name %s", dbType.getId(), dbType.getName()));
    } else {
      typesById.put(typeKey, dbType);
    }
  }

  @Override
  public DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException {
    if (!initialised) {
      synchronized (lock) {
        if (!initialised) {
          initialise(connection);
          initialised = true;
        }

      }
    }

    String typeKey = name + id;
    if (typesById.containsKey(typeKey)) {
      return typesById.get(typeKey);
    } else if (id == Types.OTHER) {
      return UnknownDbType.getInstance();
    } else {
      throw new UnknownDbTypeException(id, name);
    }
  }

  @Override
  public DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException {
    throw new UnknownDbTypeException(name);
  }

  protected void initialise(DbConnection connection) {
    try {
      DatabaseMetaData metaData = connection.getJdbcConnection().getMetaData();
      ResultSet typeInfo = metaData.getTypeInfo();
      ResultSetIterator resultSetIterator =
          new ResultSetIterator(connection, typeInfo, new InsensitiveMapRowHandler(), new SingleResultResultSetCloser(false));
      while (resultSetIterator.hasNext()) {
        Map<String, Object> typeRecord = resultSetIterator.next();

        Number dataType = (Number) typeRecord.get(METADATA_TYPE_ID_COLUMN);
        String typeName = (String) typeRecord.get(METADATA_TYPE_NAME_COLUMN);

        DbType resolvedDbType = dataType.equals(Types.BLOB)
            ? new BlobDbType(dataType.intValue(), typeName)
            : new ResolvedDbType(dataType.intValue(), typeName);

        if (!isUserDefinedType(resolvedDbType)) {
          registerType(resolvedDbType);

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Type: " + typeRecord);
          }
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot process metadata information", e);
    }
  }

  /**
   * According to documentation STRUCT and DISTINCT types with name different than "STRUCT" and "DISTINCT" should be considered
   * user defined types: http://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo() Note: documentation
   * says nothing about ARRAY types with a different name than ARRAY, but postgres returns two different user defined types for
   * each defined table, one with the name of the table and type STRUCT, and another one with the name of the table prefixed by an
   * underscore and type ARRAY. We assume that ARRAY is behaving the same as STRUCT and DISTINCT in this aspect.
   */
  private boolean isUserDefinedType(DbType dbType) {
    return isTypeDerivedFrom(dbType, STRUCT.getDbType()) ||
        isTypeDerivedFrom(dbType, DISTINCT.getDbType()) ||
        isTypeDerivedFrom(dbType, ARRAY.getDbType());
  }

  private boolean isTypeDerivedFrom(DbType type, DbType baseType) {
    return type.getId() == baseType.getId() && !type.getName().equals(baseType.getName());
  }
}
