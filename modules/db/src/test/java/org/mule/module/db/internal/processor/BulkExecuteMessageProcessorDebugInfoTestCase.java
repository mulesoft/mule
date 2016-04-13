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
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.debug.DbDebugInfoTestUtils.createQueryFieldDebugInfoMatcher;
import static org.mule.module.db.internal.domain.query.QueryType.DELETE;
import static org.mule.module.db.internal.domain.query.QueryType.INSERT;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.NOT_SUPPORTED;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERIES_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.query.BulkQueryResolver;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

@SmallTest
public class BulkExecuteMessageProcessorDebugInfoTestCase extends AbstractMuleTestCase
{

    public static final String DELETE_QUERY = "DELETE FROM PLANET";
    public static final QueryTemplate QUERY_TEMPLATE1 = new QueryTemplate(DELETE_QUERY, DELETE, Collections.<QueryParam>emptyList());
    public static final String INSERT_QUERY = "INSERT INTO PLANET(POSITION, NAME) VALUES (6, 'Saturn')";
    public static final QueryTemplate QUERY_TEMPLATE2 = new QueryTemplate(INSERT_QUERY, INSERT, Collections.<QueryParam>emptyList());
    public static final String QUERY1 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 1;
    public static final String QUERY2 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 2;

    private final MuleEvent event = mock(MuleEvent.class);
    private final DbConnection connection = mock(DbConnection.class);
    private final DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);
    private final DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
    private final DbConfig dbConfig = mock(DbConfig.class);
    private final BulkQueryResolver bulkQueryResolver = mock(BulkQueryResolver.class);

    @Test
    public void returnsDebugInfo() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        final BulkQuery bulkQuery = new BulkQuery();
        bulkQuery.add(new QueryTemplate(DELETE_QUERY, DELETE, Collections.<QueryParam>emptyList()));
        bulkQuery.add(new QueryTemplate(INSERT_QUERY, QueryType.INSERT, Collections.<QueryParam>emptyList()));
        when(bulkQueryResolver.resolve(event)).thenReturn(bulkQuery);

        final BulkExecuteMessageProcessor bulkExecuteMessageProcessor = new BulkExecuteMessageProcessor(dbConfigResolver, bulkQueryResolver, null, NOT_SUPPORTED);

        final List<FieldDebugInfo<?>> debugInfo = bulkExecuteMessageProcessor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(objectLike(QUERIES_DEBUG_FIELD, List.class, createExpectedQueryMatchers())));
    }

    @Test
    public void returnsErrorDebugInfoWhenCannotResolveQueries() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        final QueryResolutionException queryResolutionException = new QueryResolutionException("Error");
        when(bulkQueryResolver.resolve(event)).thenThrow(queryResolutionException);

        final BulkExecuteMessageProcessor bulkExecuteMessageProcessor = new BulkExecuteMessageProcessor(dbConfigResolver, bulkQueryResolver, null, NOT_SUPPORTED);

        final List<FieldDebugInfo<?>> debugInfo = bulkExecuteMessageProcessor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(fieldLike(QUERIES_DEBUG_FIELD, List.class, queryResolutionException)));
    }

    @Test
    public void returnsErrorDebugInfoOnInvalidResolvedQuery() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        final BulkQuery bulkQuery = new BulkQuery();
        bulkQuery.add(new QueryTemplate(DELETE_QUERY, QueryType.SELECT, Collections.<QueryParam>emptyList()));
        bulkQuery.add(new QueryTemplate(INSERT_QUERY, QueryType.INSERT, Collections.<QueryParam>emptyList()));
        when(bulkQueryResolver.resolve(event)).thenReturn(bulkQuery);

        final BulkExecuteMessageProcessor bulkExecuteMessageProcessor = new BulkExecuteMessageProcessor(dbConfigResolver, bulkQueryResolver, null, NOT_SUPPORTED);

        final List<FieldDebugInfo<?>> debugInfo = bulkExecuteMessageProcessor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        final FieldDebugInfo<?> fieldDebugInfo = debugInfo.get(0);
        assertThat(fieldDebugInfo.getName(), equalTo(QUERIES_DEBUG_FIELD));
        assertThat(fieldDebugInfo.getType(), equalTo(List.class.getName()));
        assertThat(fieldDebugInfo.getValue(), instanceOf(IllegalArgumentException.class));
    }

    private List<Matcher<FieldDebugInfo<?>>> createExpectedQueryMatchers()
    {
        final List<Matcher<FieldDebugInfo<?>>> queriesDebugInfo = new ArrayList<>();
        queriesDebugInfo.add(createQueryFieldDebugInfoMatcher(QUERY1, QUERY_TEMPLATE1));
        queriesDebugInfo.add(createQueryFieldDebugInfoMatcher(QUERY2, QUERY_TEMPLATE2));

        return queriesDebugInfo;
    }
}
