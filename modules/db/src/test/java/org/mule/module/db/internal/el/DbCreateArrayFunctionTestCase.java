/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import com.mysql.jdbc.PreparedStatement;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.connection.DefaultDbConnection;
import org.mule.module.db.internal.domain.connection.DefaultDbConnectionReleaser;
import org.mule.module.db.internal.domain.connection.OracleDbConnection;
import org.mule.module.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.tck.size.SmallTest;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.connection.DefaultDbConnection.ATTR_TYPE_NAME_INDEX;
import static org.mule.module.db.internal.domain.connection.DefaultDbConnection.DATA_TYPE_INDEX;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.ATTR_NO_PARAM;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.ATTR_TYPE_NAME_PARAM;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.QUERY_TYPE_ATTRS;
import static org.mule.module.db.internal.domain.connection.type.resolver.CollectionTypeResolver.QUERY_ALL_COLL_TYPES;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.ALWAYS_JOIN;
import static org.mule.module.db.internal.domain.type.JdbcTypes.BLOB_DB_TYPE;
import static org.mule.module.db.internal.domain.type.JdbcTypes.CLOB_DB_TYPE;

@SmallTest
public class DbCreateArrayFunctionTestCase extends AbstractDbCreateFunctionTestCase
{

    @Test
    public void createsDbArrayFromJavaArray() throws Exception
    {
        Object[] structValue = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, structValue};

        DbConnection dbConnection = createDbConnection(true);

        Array array = mock(Array.class);
        when(dbConnection.createArrayOf(TYPE_NAME, structValue)).thenReturn(array);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(array));
    }

    @Test
    public void createsDbArrayFromList() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, Arrays.asList(structValues)};

        DbConnection dbConnection = createDbConnection(true);

        Array array = mock(Array.class);
        when(dbConnection.createArrayOf(TYPE_NAME, structValues)).thenReturn(array);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(array));
    }

    @Test
    public void createsDbArrayResolvingClobWithOracleConnection() throws Exception
    {
        Object[] structValues = {"clob", "foo"};
        Object[] structValues1 = {"clob1", "foo1"};
        Object[] params = {structValues, structValues1};

        Connection connection = mock(Connection.class);
        Clob clob = mock(Clob.class);
        when(connection.createClob()).thenReturn(clob);

        Array array = mock(Array.class);
        when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

        testThroughOracleQuery(connection, params, CLOB_DB_TYPE.getName(), TYPE_NAME);

        verify(connection).createArrayOf(TYPE_NAME, params);
        assertThat(((Object[]) params[0])[0], Matchers.<Object> equalTo(clob));
        assertThat(((Object[]) params[1])[0], Matchers.<Object> equalTo(clob));
    }

    @Test
    public void createsDbArrayResolvingBlobWithOracleConnection() throws Exception
    {
        Object[] structValues = {"blob", "foo"};
        Object[] structValues1 = {"blob1", "foo1"};
        Object[] params = {structValues, structValues1};

        Connection connection = mock(Connection.class);
        Blob blob = mock(Blob.class);
        when(connection.createBlob()).thenReturn(blob);

        Array array = mock(Array.class);
        when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

        testThroughOracleQuery(connection, params, BLOB_DB_TYPE.getName(), TYPE_NAME);

        verify(connection).createArrayOf(TYPE_NAME, params);
        assertThat(((Object[]) params[0])[0], Matchers.<Object> equalTo(blob));
        assertThat(((Object[]) params[1])[0], Matchers.<Object> equalTo(blob));
    }

    private void testThroughOracleQuery(Connection delegate, Object[] structValues, String dataTypeName, String udtName) throws Exception
    {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(delegate.prepareStatement(QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);

        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getInt(ATTR_NO_PARAM)).thenReturn(1);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(dataTypeName);

        DbConnectionFactory connectionFactory = mock(DbConnectionFactory.class);
        DefaultDbConnectionReleaser releaser = new DefaultDbConnectionReleaser(connectionFactory);
        ParamTypeResolverFactory paramTypeResolverFactory = mock(ParamTypeResolverFactory.class);
        OracleDbConnection defaultDbConnection = new OracleDbConnection(delegate, ALWAYS_JOIN, releaser, paramTypeResolverFactory);

        defaultDbConnection.createArrayOf(udtName, structValues);
        defaultDbConnection.close();

        verify(preparedStatement, times(1)).setString(1, udtName);
    }

    @Test
    public void createsDbArrayResolvingClobWithDefaultConnection() throws Exception
    {
        Object[] structValues = {"clob", "foo"};
        Object[] structValues1 = {"clob1", "foo1"};
        Object[] params = {structValues, structValues1};

        Connection connection = mock(Connection.class);
        Clob clob = mock(Clob.class);
        when(connection.createClob()).thenReturn(clob);

        Array array = mock(Array.class);
        when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

        DefaultDbConnection defaultDbConnection = testThroughMetadata(connection, params, CLOB_DB_TYPE.getId(), CLOB_DB_TYPE.getName());

        defaultDbConnection.createArrayOf(TYPE_NAME, params);
        defaultDbConnection.close();

        verify(connection).createArrayOf(TYPE_NAME, params);
        assertThat(((Object[]) params[0])[0], Matchers.<Object> equalTo(clob));
        assertThat(((Object[]) params[1])[0], Matchers.<Object> equalTo(clob));
    }

    @Test
    public void createsDbArrayResolvingBlobWithDefaultConnection() throws Exception
    {
        Object[] structValues = {"blob", "foo"};
        Object[] structValues1 = {"blob1", "foo1"};
        Object[] params = {structValues, structValues1};

        Connection connection = mock(Connection.class);
        Blob blob = mock(Blob.class);
        when(connection.createBlob()).thenReturn(blob);

        Array array = mock(Array.class);
        when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

        DefaultDbConnection defaultDbConnection = testThroughMetadata(connection, params, BLOB_DB_TYPE.getId(),  BLOB_DB_TYPE.getName());

        defaultDbConnection.createArrayOf(TYPE_NAME, params);
        defaultDbConnection.close();

        verify(connection).createArrayOf(TYPE_NAME, params);
        assertThat(((Object[]) params[0])[0], Matchers.<Object> equalTo(blob));
        assertThat(((Object[]) params[1])[0], Matchers.<Object> equalTo(blob));
    }

    protected DefaultDbConnection testThroughMetadata(Connection delegate, Object[] structValues, int dataType, String dataTypeName) throws Exception
    {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(delegate.getMetaData()).thenReturn(metadata);
        when(delegate.getCatalog()).thenReturn("catalog");
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getInt(DATA_TYPE_INDEX)).thenReturn(dataType);
        when(resultSet.getString(ATTR_TYPE_NAME_INDEX)).thenReturn(dataTypeName);
        when(metadata.getAttributes("catalog", null, TYPE_NAME, null)).thenReturn(resultSet);

        DbConnectionFactory connectionFactory = mock(DbConnectionFactory.class);
        DefaultDbConnectionReleaser releaser = new DefaultDbConnectionReleaser(connectionFactory);
        ParamTypeResolverFactory paramTypeResolverFactory = mock(ParamTypeResolverFactory.class);
        return new DefaultDbConnection(delegate, ALWAYS_JOIN, releaser, paramTypeResolverFactory);
    }

    @Override
    protected AbstractDbFunction createDbFunction(MuleContext muleContext)
    {
        return new DbCreateArrayFunction(muleContext);
    }
}
