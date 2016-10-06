/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static java.sql.Types.VARCHAR;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseDbMetadataResolver {

  protected BaseTypeBuilder typeBuilder;
  protected ClassTypeLoader typeLoader;
  private Map<Integer, MetadataType> dbToMetaDataType;

  protected QueryTemplate parseQuery(String query) {
    return new SimpleQueryTemplateParser().parse(query);
  }

  protected PreparedStatement getStatement(MetadataContext context, QueryTemplate query)
      throws ConnectionException, MetadataResolvingException {
    DbConnection connection = context.<DbConnection>getConnection()
        .orElseThrow(() -> new MetadataResolvingException("A connection is required to resolve Metadata but none was provided",
                                                          INVALID_CONFIGURATION));
    PreparedStatement statement;
    try {
      statement = connection.getJdbcConnection().prepareStatement(query.getSqlText());
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), UNKNOWN, e);
    }
    return statement;
  }

  protected MetadataType getDataTypeMetadataModel(int columnTypeName, String columnClassName) {
    if (columnTypeName == Types.JAVA_OBJECT) {
      return typeLoader.load(columnClassName).orElse(typeBuilder.anyType().build());
    }

    return getDataTypeMetadataModel(columnTypeName);
  }

  protected MetadataType getDataTypeMetadataModel(int columnTypeName) {
    if (dbToMetaDataType == null) {
      synchronized (this) {
        if (dbToMetaDataType == null) {
          initializeDbToMetaDataType();
        }
      }
    }

    return dbToMetaDataType.getOrDefault(columnTypeName, typeBuilder.anyType().build());
  }

  private void initializeDbToMetaDataType() {
    dbToMetaDataType = new HashMap<>();

    dbToMetaDataType.put(Types.BIT, typeBuilder.booleanType().build());
    dbToMetaDataType.put(Types.TINYINT, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.SMALLINT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.INTEGER, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.BIGINT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.FLOAT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.REAL, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.DOUBLE, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.NUMERIC, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.DECIMAL, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.CHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(VARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.LONGNVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.DATE, typeLoader.load(Date.class));
    dbToMetaDataType.put(Types.TIME, typeLoader.load(Time.class));
    dbToMetaDataType.put(Types.TIMESTAMP, typeLoader.load(Timestamp.class));
    dbToMetaDataType.put(Types.BINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.VARBINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.LONGVARBINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.NULL, typeBuilder.nullType().build());
    dbToMetaDataType.put(Types.OTHER, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.DISTINCT, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.STRUCT, typeLoader.load(Struct.class));
    dbToMetaDataType.put(Types.ARRAY, typeBuilder.arrayType().of().anyType().build());
    dbToMetaDataType.put(Types.BLOB, typeLoader.load(Blob.class));
    dbToMetaDataType.put(Types.CLOB, typeLoader.load(Clob.class));
    dbToMetaDataType.put(Types.REF, typeLoader.load(Ref.class));
    dbToMetaDataType.put(Types.DATALINK, typeLoader.load(URL.class));
    dbToMetaDataType.put(Types.BOOLEAN, typeBuilder.booleanType().build());
    dbToMetaDataType.put(Types.ROWID, typeLoader.load(RowId.class));
    dbToMetaDataType.put(Types.NCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.NVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.LONGNVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.NCLOB, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.SQLXML, typeLoader.load(SQLXML.class));
  }
}
