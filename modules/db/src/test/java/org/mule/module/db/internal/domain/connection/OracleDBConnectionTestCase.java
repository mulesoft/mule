/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import com.mysql.jdbc.PreparedStatement;
import org.junit.Test;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.*;
import static org.mule.module.db.internal.domain.connection.OracleDbConnection.ATTR_TYPE_NAME_PARAM;
import static org.mule.module.db.internal.domain.connection.type.resolver.CollectionTypeResolver.QUERY_ALL_COLL_TYPES;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.ALWAYS_JOIN;

public class OracleDBConnectionTestCase extends AbstractMuleTestCase
{
    private final String TYPE_NAME = "TYPE_NAME";
    private final String OTHER_TYPE_NAME = "OTHER_TYPE";


    @Test
    public void lobResolutionPerformance() throws Exception
    {

        Map<String, Map<Integer, ResolvedDbType>> dbTypeCache = new ConcurrentHashMap<>();
        Object[] structValues = {"clob", "foo"};
        Object[] structValues1 = {"clob1", "foo1"};
        Object[] params = {structValues, structValues1};
        Object[] params2 = {structValues, structValues1};

        Connection delegate = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(delegate.prepareStatement(QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);

        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(TYPE_NAME);

        ParamTypeResolverFactory paramTypeResolverFactory = mock(ParamTypeResolverFactory.class);

        DbConnectionFactory connectionFactory = mock(DbConnectionFactory.class);
        DefaultDbConnectionReleaser releaser = new DefaultDbConnectionReleaser(connectionFactory);

        when(connectionFactory.createConnection(ALWAYS_JOIN))
            .thenReturn(new OracleDbConnection(delegate, ALWAYS_JOIN, releaser, paramTypeResolverFactory, dbTypeCache));

        DbConnection defaultDbConnection = connectionFactory.createConnection(ALWAYS_JOIN);
        defaultDbConnection.createArrayOf(TYPE_NAME, params);
        defaultDbConnection.close();
        assertThat(dbTypeCache.containsKey(TYPE_NAME), is(true));
        verify(preparedStatement, times(2)).executeQuery();

        DbConnection anotherDbConnection = connectionFactory.createConnection(ALWAYS_JOIN);

        defaultDbConnection.createArrayOf(OTHER_TYPE_NAME, params2);
        defaultDbConnection.close();
        assertThat(dbTypeCache.containsKey(OTHER_TYPE_NAME), is(true));
        verify(preparedStatement, times(4)).executeQuery();

        DbConnection oneMoreDbConnection = connectionFactory.createConnection(ALWAYS_JOIN);
        defaultDbConnection.createArrayOf(OTHER_TYPE_NAME, params);
        defaultDbConnection.close();
        assertThat(dbTypeCache.containsKey(OTHER_TYPE_NAME), is(true));
        verify(preparedStatement, times(5)).executeQuery();

    }
}
