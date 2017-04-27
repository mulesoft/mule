/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 *  Defines {@link DbType} for JDBC types defined in {@link java.sql.Types}
 */
public class JdbcTypes
{

    public static final DbType BIT_DB_TYPE = new ResolvedDbType(Types.BIT, "BIT");
    public static final DbType TINYINT_DB_TYPE = new ResolvedDbType(Types.TINYINT, "TINYINT");
    public static final DbType SMALLINT_DB_TYPE = new ResolvedDbType(Types.SMALLINT, "SMALLINT");
    public static final DbType INTEGER_DB_TYPE = new ResolvedDbType(Types.INTEGER, "INTEGER");
    public static final DbType BIGINT_DB_TYPE = new ResolvedDbType(Types.BIGINT, "BIGINT");
    public static final DbType FLOAT_DB_TYPE = new ResolvedDbType(Types.FLOAT, "FLOAT");
    public static final DbType REAL_DB_TYPE = new ResolvedDbType(Types.REAL, "REAL");
    public static final DbType DOUBLE_DB_TYPE = new ResolvedDbType(Types.DOUBLE, "DOUBLE");
    public static final DbType NUMERIC_DB_TYPE = new ResolvedDbType(Types.NUMERIC, "NUMERIC");
    public static final DbType DECIMAL_DB_TYPE = new ResolvedDbType(Types.DECIMAL, "DECIMAL");
    public static final DbType CHAR_DB_TYPE = new ResolvedDbType(Types.CHAR, "CHAR");
    public static final DbType VARCHAR_DB_TYPE = new ResolvedDbType(Types.VARCHAR, "VARCHAR");
    public static final DbType LONGVARCHAR_DB_TYPE = new ResolvedDbType(Types.LONGVARCHAR, "LONGVARCHAR");
    public static final DbType DATE_DB_TYPE = new ResolvedDbType(Types.DATE, "DATE");
    public static final DbType TIME_DB_TYPE = new ResolvedDbType(Types.TIME, "TIME");
    public static final DbType TIMESTAMP_DB_TYPE = new ResolvedDbType(Types.TIMESTAMP, "TIMESTAMP");
    public static final DbType BINARY_DB_TYPE = new ResolvedDbType(Types.BINARY, "BINARY");
    public static final DbType VARBINARY_DB_TYPE = new ResolvedDbType(Types.VARBINARY, "VARBINARY");
    public static final DbType LONGVARBINARY_DB_TYPE = new ResolvedDbType(Types.LONGVARBINARY, "LONGVARBINARY");
    public static final DbType NULL_DB_TYPE = new ResolvedDbType(Types.NULL, "NULL");
    public static final DbType OTHER_DB_TYPE = new ResolvedDbType(Types.OTHER, "OTHER");
    public static final DbType JAVA_OBJECT_DB_TYPE = new ResolvedDbType(Types.JAVA_OBJECT, "JAVA_OBJECT");
    public static final DbType DISTINCT_DB_TYPE = new ResolvedDbType(Types.DISTINCT, "DISTINCT");
    public static final DbType STRUCT_DB_TYPE = new ResolvedDbType(Types.STRUCT, "STRUCT");
    public static final DbType ARRAY_DB_TYPE = new ArrayResolvedDbType(Types.ARRAY, "ARRAY");
    public static final DbType BLOB_DB_TYPE = new BlobResolvedDataType(Types.BLOB, "BLOB");
    public static final DbType CLOB_DB_TYPE = new ClobResolvedDataType(Types.CLOB, "CLOB");
    public static final DbType REF_DB_TYPE = new ResolvedDbType(Types.REF, "REF");
    public static final DbType DATALINK_DB_TYPE = new ResolvedDbType(Types.DATALINK, "DATALINK");
    public static final DbType BOOLEAN_DB_TYPE = new ResolvedDbType(Types.BOOLEAN, "BOOLEAN");
    public static final DbType ROWID_DB_TYPE = new ResolvedDbType(Types.ROWID, "ROWID");
    public static final DbType NCHAR_DB_TYPE = new ResolvedDbType(Types.NCHAR, "NCHAR");
    public static final DbType NVARCHAR_DB_TYPE = new ResolvedDbType(Types.NVARCHAR, "NVARCHAR");
    public static final DbType LONGNVARCHAR_DB_TYPE = new ResolvedDbType(Types.LONGNVARCHAR, "LONGNVARCHAR");
    public static final DbType NCLOB_DB_TYPE = new ResolvedDbType(Types.NCLOB, "NCLOB");
    public static final DbType SQLXML_DB_TYPE = new ResolvedDbType(Types.SQLXML, "SQLXML");

    public static final List<DbType> types;

    static
    {
        types = new ArrayList<DbType>();

        Field[] fields = JdbcTypes.class.getDeclaredFields();

        for (Field declaredField : fields)
        {
            try
            {
                Object value = declaredField.get(JdbcTypes.class);

                if (value instanceof DbType)
                {
                    DbType dbType = (DbType) value;
                    types.add(dbType);
                }
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException("Unable to initialize JDBC types", e);
            }
        }
    }
}
