/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.database.UnresolvableDbConfigException;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.module.db.internal.resolver.query.QueryResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

public abstract class AbstractSingleQueryMessageProcessorDebugInfoTestCase extends AbstractMuleTestCase
{

    protected final MuleEvent event = mock(MuleEvent.class);
    protected final DbConnection connection = mock(DbConnection.class);
    protected final DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);
    protected final DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
    protected final DbConfig dbConfig = mock(DbConfig.class);
    protected final QueryResolver queryResolver = mock(QueryResolver.class);

    @Test
    public void returnsErrorDebugInfoWhenCannotResolveConfig() throws Exception
    {
        final UnresolvableDbConfigException connectionErrorDebugInfo = new UnresolvableDbConfigException("Error");

        when(dbConfigResolver.resolve(event)).thenThrow(connectionErrorDebugInfo);

        AbstractSingleQueryDbMessageProcessor processor = createMessageProcessor();

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(fieldLike("Config", DbConfig.class, connectionErrorDebugInfo)));
    }

    @Test
    public void returnsErrorDebugInfoWhenCannotObtainConnection() throws Exception
    {
        final SQLException debugInfoError = new SQLException();

        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenThrow(debugInfoError);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        AbstractSingleQueryDbMessageProcessor processor = createMessageProcessor();

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(fieldLike("Connection", DbConnection.class, debugInfoError)));
    }

    @Test
    public void returnsErrorDebugInfoWhenCannotResolveQuery() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        final QueryResolutionException resolutionException = new QueryResolutionException("Error");
        when(queryResolver.resolve(connection, event)).thenThrow(resolutionException);

        AbstractSingleQueryDbMessageProcessor processor = createMessageProcessor();

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, resolutionException)));

    }

    protected abstract AbstractSingleQueryDbMessageProcessor createMessageProcessor();

    protected abstract String getSqlText();

    protected abstract QueryType getQueryType();
}
