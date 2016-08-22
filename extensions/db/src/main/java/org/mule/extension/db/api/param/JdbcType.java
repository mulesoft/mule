/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.api.param;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;

import java.sql.Types;
import java.util.List;

/**
 * Defines {@link DbType} for JDBC types defined in {@link Types}
 *
 * @since 4.0
 */
public enum JdbcType {

  BIT(new ResolvedDbType(Types.BIT, "BIT")), TINYINT(
      new ResolvedDbType(Types.TINYINT, "TINYINT")), SMALLINT(
          new ResolvedDbType(Types.SMALLINT, "SMALLINT")), INTEGER(
              new ResolvedDbType(Types.INTEGER, "INTEGER")), BIGINT(
                  new ResolvedDbType(Types.BIGINT, "BIGINT")), FLOAT(
                      new ResolvedDbType(Types.FLOAT, "FLOAT")), REAL(
                          new ResolvedDbType(Types.REAL, "REAL")), DOUBLE(
                              new ResolvedDbType(Types.DOUBLE, "DOUBLE")), NUMERIC(
                                  new ResolvedDbType(Types.NUMERIC, "NUMERIC")), DECIMAL(
                                      new ResolvedDbType(Types.DECIMAL, "DECIMAL")), CHAR(
                                          new ResolvedDbType(Types.CHAR, "CHAR")), VARCHAR(
                                              new ResolvedDbType(Types.VARCHAR, "VARCHAR")), LONGVARCHAR(
                                                  new ResolvedDbType(Types.LONGVARCHAR, "LONGVARCHAR")), DATE(
                                                      new ResolvedDbType(Types.DATE, "DATE")), TIME(
                                                          new ResolvedDbType(Types.TIME, "TIME")), TIMESTAMP(
                                                              new ResolvedDbType(Types.TIMESTAMP, "TIMESTAMP")), BINARY(
                                                                  new ResolvedDbType(Types.BINARY, "BINARY")), VARBINARY(
                                                                      new ResolvedDbType(Types.VARBINARY,
                                                                                         "VARBINARY")), LONGVARBINARY(
                                                                                             new ResolvedDbType(Types.LONGVARBINARY,
                                                                                                                "LONGVARBINARY")), NULL(
                                                                                                                    new ResolvedDbType(Types.NULL,
                                                                                                                                       "NULL")), OTHER(
                                                                                                                                           new ResolvedDbType(Types.OTHER,
                                                                                                                                                              "OTHER")), JAVA_OBJECT(
                                                                                                                                                                  new ResolvedDbType(Types.JAVA_OBJECT,
                                                                                                                                                                                     "JAVA_OBJECT")), DISTINCT(
                                                                                                                                                                                         new ResolvedDbType(Types.DISTINCT,
                                                                                                                                                                                                            "DISTINCT")), STRUCT(
                                                                                                                                                                                                                new ResolvedDbType(Types.STRUCT,
                                                                                                                                                                                                                                   "STRUCT")), ARRAY(
                                                                                                                                                                                                                                       new ResolvedDbType(Types.ARRAY,
                                                                                                                                                                                                                                                          "ARRAY")), BLOB(
                                                                                                                                                                                                                                                              new ResolvedDbType(Types.BLOB,
                                                                                                                                                                                                                                                                                 "BLOB")), CLOB(
                                                                                                                                                                                                                                                                                     new ResolvedDbType(Types.CLOB,
                                                                                                                                                                                                                                                                                                        "CLOB")), REF(
                                                                                                                                                                                                                                                                                                            new ResolvedDbType(Types.REF,
                                                                                                                                                                                                                                                                                                                               "REF")), DATALINK(
                                                                                                                                                                                                                                                                                                                                   new ResolvedDbType(Types.DATALINK,
                                                                                                                                                                                                                                                                                                                                                      "DATALINK")), BOOLEAN(
                                                                                                                                                                                                                                                                                                                                                          new ResolvedDbType(Types.BOOLEAN,
                                                                                                                                                                                                                                                                                                                                                                             "BOOLEAN")), ROWID(
                                                                                                                                                                                                                                                                                                                                                                                 new ResolvedDbType(Types.ROWID,
                                                                                                                                                                                                                                                                                                                                                                                                    "ROWID")), NCHAR(
                                                                                                                                                                                                                                                                                                                                                                                                        new ResolvedDbType(Types.NCHAR,
                                                                                                                                                                                                                                                                                                                                                                                                                           "NCHAR")), NVARCHAR(
                                                                                                                                                                                                                                                                                                                                                                                                                               new ResolvedDbType(Types.NVARCHAR,
                                                                                                                                                                                                                                                                                                                                                                                                                                                  "NVARCHAR")), LONGNVARCHAR(
                                                                                                                                                                                                                                                                                                                                                                                                                                                      new ResolvedDbType(Types.LONGNVARCHAR,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                         "LONGNVARCHAR")), NCLOB(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                             new ResolvedDbType(Types.NCLOB,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "NCLOB")), SQLXML(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    new ResolvedDbType(Types.SQLXML,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       "SQLXML"));

  private final DbType dbType;

  JdbcType(DbType dbType) {
    this.dbType = dbType;
  }

  public static List<DbType> getAllTypes() {
    return stream(JdbcType.values()).map(JdbcType::getDbType).collect(toList());
  }

  public DbType getDbType() {
    return dbType;
  }
}
