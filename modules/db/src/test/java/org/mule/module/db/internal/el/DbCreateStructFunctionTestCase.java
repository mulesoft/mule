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

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Struct;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.ATTR_NO_PARAM;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.ATTR_TYPE_NAME_PARAM;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.QUERY_TYPE_ATTRS;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.QUERY_TYPE_OWNER_CONDITION;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.ALWAYS_JOIN;
import static org.mule.module.db.internal.domain.type.JdbcTypes.BLOB_DB_TYPE;
import static org.mule.module.db.internal.domain.type.JdbcTypes.CLOB_DB_TYPE;

@SmallTest
public class DbCreateStructFunctionTestCase extends AbstractDbCreateFunctionTestCase
{

    @Test
    public void createsStructFromArray() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, structValues};

        DbConnection dbConnection = createDbConnection(true);

        Struct struct = mock(Struct.class);
        when(dbConnection.createStruct(TYPE_NAME, structValues)).thenReturn(struct);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(struct));
    }

    @Test
    public void createsStructFromList() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, Arrays.asList(structValues)};

        DbConnection dbConnection = createDbConnection(true);

        Struct struct = mock(Struct.class);
        when(dbConnection.createStruct(TYPE_NAME, structValues)).thenReturn(struct);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(struct));
    }
    
    @Test
    public void createStructResolvingBlobInDefaultDbConnection() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Connection delegate = mock(Connection.class);
        Blob blob = mock(Blob.class);
        when(delegate.createBlob()).thenReturn(blob);

        DefaultDbConnection defaultDbConnection = testThroughMetadata(delegate, structValues, BLOB_DB_TYPE.getId(), BLOB_DB_TYPE.getName());

        defaultDbConnection.createStruct(TYPE_NAME, structValues);
        defaultDbConnection.close();

        verify(delegate).createStruct(TYPE_NAME, structValues);
        assertThat(structValues[0], Matchers.<Object>equalTo(blob));
    }
    
    @Test
    public void createStructResolvingClobInDefaultDbConnection() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Connection delegate = mock(Connection.class);
        Clob clob = mock(Clob.class);
        when(delegate.createClob()).thenReturn(clob);

        DefaultDbConnection defaultDbConnection = testThroughMetadata(delegate, structValues, CLOB_DB_TYPE.getId(), CLOB_DB_TYPE.getName());

        defaultDbConnection.createStruct(TYPE_NAME, structValues);
        defaultDbConnection.close();

        verify(delegate).createStruct(TYPE_NAME, structValues);
        assertThat(structValues[0], Matchers.<Object>equalTo(clob));
    }
    
    @Test
    public void createStructResolvingBlobInOracleDbUsingUDTSimpleName() throws Exception
    {
        createStructResolvingBlobInOracleDb(TYPE_NAME);
    }

    @Test
    public void createStructResolvingBlobInOracleDbUsingUDTFullName() throws Exception
    {
        createStructResolvingBlobInOracleDb(TYPE_NAME_WITH_OWNER);
    }

    @Test
    public void createStructResolvingClobInOracleDbUsingUDTSimpleName() throws Exception
    {
        createStructResolvingClobAndClobInOracleDb(TYPE_NAME);
    }

    @Test
    public void createStructResolvingClobInOracleDbUsingUDTFullName() throws Exception
    {
        createStructResolvingClobAndClobInOracleDb(TYPE_NAME_WITH_OWNER);
    }

    public void createStructResolvingBlobInOracleDb(String typeName) throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Connection delegate = mock(Connection.class);
        Blob blob = mock(Blob.class);
        when(delegate.createBlob()).thenReturn(blob);
        testThroughOracleQuery(delegate, structValues, BLOB_DB_TYPE.getName(), typeName);
        verify(delegate).createStruct(typeName, structValues);
        assertThat(structValues[0], Matchers.<Object> equalTo(blob));
    }

    public void createStructResolvingClobAndClobInOracleDb(String typeName) throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Connection delegate = mock(Connection.class);
        Clob clob = mock(Clob.class);
        when(delegate.createClob()).thenReturn(clob);
        testThroughOracleQuery(delegate, structValues, CLOB_DB_TYPE.getName(), typeName);
        verify(delegate).createStruct(typeName, structValues);
        assertThat(structValues[0], Matchers.<Object> equalTo(clob));
    }

    private void testThroughOracleQuery(Connection delegate, Object[] structValues, String dataTypeName, String udtName) throws Exception
    {
        String owner = getOwnerFrom(udtName);
        String typeSimpleName = getTypeSimpleName(udtName);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        if (owner == null)
        {
            when(delegate.prepareStatement(QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        }
        else
        {
            when(delegate.prepareStatement(QUERY_TYPE_ATTRS + QUERY_TYPE_OWNER_CONDITION)).thenReturn(preparedStatement);
        }

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getInt(ATTR_NO_PARAM)).thenReturn(1);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(dataTypeName);

        DbConnectionFactory connectionFactory = mock(DbConnectionFactory.class);
        DefaultDbConnectionReleaser releaser = new DefaultDbConnectionReleaser(connectionFactory);
        ParamTypeResolverFactory paramTypeResolverFactory = mock(ParamTypeResolverFactory.class);
        OracleDbConnection defaultDbConnection = new OracleDbConnection(delegate, ALWAYS_JOIN, releaser, paramTypeResolverFactory);
        defaultDbConnection.createStruct(udtName, structValues);
        defaultDbConnection.close();

        verify(preparedStatement).setString(1, typeSimpleName);

        if (owner != null)
        {
            verify(preparedStatement).setString(2, owner);
        }
    }

    @Override
    protected AbstractDbFunction createDbFunction(MuleContext muleContext)
    {
        return new DbCreateStructFunction(muleContext);
    }
}
