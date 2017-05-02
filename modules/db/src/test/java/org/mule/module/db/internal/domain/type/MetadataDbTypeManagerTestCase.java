/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.test.util.ColumnMetadata;
import org.mule.module.db.test.util.ResultSetBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class MetadataDbTypeManagerTestCase extends AbstractMuleTestCase
{

    private static final DbType UDT_ARRAY = new ResolvedDbType(Types.ARRAY, "UDT_ARRAY");
    private static final DbType UDT_DISTINCT = new ResolvedDbType(Types.DISTINCT, "UDT_DISTINCT");
    private static final DbType UDT_STRUCT = new ResolvedDbType(Types.STRUCT, "UDT_STRUCT");
    private static final DbType UDT_OK = new ResolvedDbType(1, "UDT_OK");
    private static final DbType UDT_CLOB = new ResolvedDbType(Types.CLOB, "CLOB");
    private static final DbType UDT_BLOB = new ResolvedDbType(Types.BLOB, "BLOB");

    @Test
    public void ignoreUserDefinedTypes() throws Exception
    {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getTypeInfo()).thenReturn(createResultSetWithUserDefinedTypes());

        DbConnection connection = mock(DbConnection.class);
        when(connection.getMetaData()).thenReturn(metaData);

        MetadataDbTypeManager typeManager = new MetadataDbTypeManager();

        assertThat(typeManager.lookup(connection, UDT_OK.getId(), UDT_OK.getName()), instanceOf(DbType.class));

        assertNotContainsUserDefinedType(typeManager, connection, UDT_ARRAY);
        assertNotContainsUserDefinedType(typeManager, connection, UDT_DISTINCT);
        assertNotContainsUserDefinedType(typeManager, connection, UDT_STRUCT);
    }

    @Test
    public void resolveBlobDatatype() throws Exception
    {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getTypeInfo()).thenReturn(createResultSetWithBlobType());

        DbConnection connection = mock(DbConnection.class);
        when(connection.getMetaData()).thenReturn(metaData);

        MetadataDbTypeManager typeManager = new MetadataDbTypeManager();
        typeManager.initialise(connection);

        assertThat(typeManager.lookup(connection, Types.BLOB, "BLOB"), instanceOf(BlobResolvedDataType.class));
    }
    
    @Test
    public void resolveClobDatatype() throws Exception
    {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getTypeInfo()).thenReturn(createResultSetWithClobType());

        DbConnection connection = mock(DbConnection.class);
        when(connection.getMetaData()).thenReturn(metaData);

        MetadataDbTypeManager typeManager = new MetadataDbTypeManager();
        typeManager.initialise(connection);

        assertThat(typeManager.lookup(connection, Types.CLOB, "CLOB"), instanceOf(ClobResolvedDataType.class));
    }
    
    @Test
    public void ignoreDuplicatedTypes() throws Exception
    {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getTypeInfo()).thenReturn(createResultSetWithDuplicatedTypes());

        DbConnection connection = mock(DbConnection.class);
        when(connection.getMetaData()).thenReturn(metaData);

        MetadataDbTypeManager typeManager = new MetadataDbTypeManager();

        assertThat(typeManager.lookup(connection, UDT_OK.getId(), UDT_OK.getName()), instanceOf(DbType.class));
    }

    private void assertNotContainsUserDefinedType(MetadataDbTypeManager typeManager, DbConnection connection, DbType udtDbType)
    {
        try
        {
            typeManager.lookup(connection, udtDbType.getId(), udtDbType.getName());
            fail("User defined types must not be registered by the MetadataDbTypeManager.");
        }
        catch (UnknownDbTypeException e)
        {
            // Expected
        }
    }

    private ResultSet createResultSetWithClobType() throws SQLException
    {
        List<ColumnMetadata> columns = getTypeMedataColumns();

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns, mock(Statement.class));

        addRecord(resultSetBuilder, UDT_CLOB);
        return resultSetBuilder.build();
    }

    private ResultSet createResultSetWithBlobType() throws SQLException
    {
        List<ColumnMetadata> columns = getTypeMedataColumns();

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns, mock(Statement.class));

        addRecord(resultSetBuilder, UDT_BLOB);
        return resultSetBuilder.build();
    }

    private ResultSet createResultSetWithUserDefinedTypes() throws SQLException
    {
        List<ColumnMetadata> columns = getTypeMedataColumns();

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns, mock(Statement.class));

        addRecord(resultSetBuilder, UDT_ARRAY);
        addRecord(resultSetBuilder, UDT_DISTINCT);
        addRecord(resultSetBuilder, UDT_STRUCT);
        addRecord(resultSetBuilder, UDT_OK);

        return resultSetBuilder.build();
    }

    private ResultSet createResultSetWithDuplicatedTypes() throws SQLException
    {
        List<ColumnMetadata> columns = getTypeMedataColumns();

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns, mock(Statement.class));

        addRecord(resultSetBuilder, UDT_OK);
        addRecord(resultSetBuilder, UDT_OK);

        return resultSetBuilder.build();
    }

    private List<ColumnMetadata> getTypeMedataColumns()
    {
        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
        columns.add(new ColumnMetadata(MetadataDbTypeManager.METADATA_TYPE_ID_COLUMN, MetadataDbTypeManager.METADATA_TYPE_ID_COLUMN, 1));
        columns.add(new ColumnMetadata(MetadataDbTypeManager.METADATA_TYPE_NAME_COLUMN, MetadataDbTypeManager.METADATA_TYPE_NAME_COLUMN, 2));
        return columns;
    }

    private void addRecord(ResultSetBuilder resultSetBuilder, DbType dbType)
    {
        Map<String, Object> record = new HashMap<String, Object>();
        record.put(MetadataDbTypeManager.METADATA_TYPE_ID_COLUMN, dbType.getId());
        record.put(MetadataDbTypeManager.METADATA_TYPE_NAME_COLUMN, dbType.getName());
        resultSetBuilder.with(record);
    }
}
