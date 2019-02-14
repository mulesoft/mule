/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.registry.MuleRegistry;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.connection.DefaultDbConnection;
import org.mule.module.db.internal.domain.connection.DefaultDbConnectionReleaser;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.connection.DefaultDbConnection.ATTR_TYPE_NAME_INDEX;
import static org.mule.module.db.internal.domain.connection.DefaultDbConnection.DATA_TYPE_INDEX;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.ALWAYS_JOIN;

public abstract class AbstractDbCreateFunctionTestCase extends AbstractMuleTestCase
{

    protected static final String DB_CONFIG_NAME = "dbConfig";
    protected static final String TYPE_NAME = "TEST_ARRAY";
    protected static final String TYPE_NAME_WITH_OWNER = "OWNER.TEST_ARRAY";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final MuleContext muleContext = mock(MuleContext.class);
    protected final ExpressionLanguageContext context = mock(ExpressionLanguageContext.class);
    protected final AbstractDbFunction function = createDbFunction(muleContext);

    @Test
    public void validatesNumberOfParameters()
    {
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME};

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(function.createInvalidArgumentCountMessage(2));

        function.call(params, context);
    }

    @Test
    public void validatesFirstParameterIsAString()
    {
        Object[] params = new Object[] {new Object(), TYPE_NAME, new Object[0]};

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(function.createInvalidDbConfigNameArgument());

        function.call(params, context);
    }

    @Test
    public void validatesSecondParameterIsAString()
    {
        Object[] params = new Object[] {DB_CONFIG_NAME, new Object(), new Object[0]};

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(function.createInvalidDbTypeMessage());

        function.call(params, context);
    }

    @Test
    public void validatesThirdParameterType()
    {
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, new Object()};

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(function.createInvalidStructValuesArgument());

        function.call(params, context);
    }

    @Test
    public void requiresActiveTransaction() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, structValues};

        expectedException.expect(IllegalStateException.class);

        createDbConnection(false);

        function.call(params, context);
    }

    /**
     * @return the function instance to use on the test.
     * @param muleContext Mule context corresponding to the artifact where the function is executed.
     */
    protected abstract AbstractDbFunction createDbFunction(MuleContext muleContext);

    protected DbConnection createDbConnection(boolean withTransaction) throws SQLException
    {
        MuleEvent event = mock(MuleEvent.class);
        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        DbConfig dbConfig = mock(DbConfig.class);
        DbConnection dbConnection = mock(DbConnection.class);
        DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);

        when(context.getVariable(MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE)).thenReturn(event);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.get(DB_CONFIG_NAME)).thenReturn(dbConfigResolver);
        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        if (withTransaction)
        {
            when(dbConnectionFactory.createConnection(TransactionalAction.ALWAYS_JOIN)).thenReturn(dbConnection);
        }
        else
        {
            when(dbConnectionFactory.createConnection(TransactionalAction.ALWAYS_JOIN)).thenThrow(new IllegalStateException("No transaction"));
        }

        return dbConnection;
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

}
