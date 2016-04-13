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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.debug.DbDebugInfoTestUtils.createQueryFieldDebugInfoMatcher;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.NOT_SUPPORTED;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERIES_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.module.db.internal.resolver.query.QueryResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

@SmallTest
public class DynamicBulkUpdateMessageProcessorTestCase extends AbstractMuleTestCase
{

    public static final QueryTemplate QUERY_TEMPLATE1 = new QueryTemplate("update PLANET set NAME='Mercury' where NAME='EARTH'", UPDATE, Collections.<QueryParam>emptyList());
    public static final QueryTemplate QUERY_TEMPLATE2 = new QueryTemplate("update PLANET set NAME='Mercury' where NAME='MARS'", UPDATE, Collections.<QueryParam>emptyList());
    public static final String QUERY1 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 1;
    public static final String QUERY2 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 2;

    private final MuleEvent event = mock(MuleEvent.class);
    private final MuleMessage message= mock(MuleMessage.class);
    private final DbConnection connection = mock(DbConnection.class);
    private final DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);
    private final DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
    private final DbConfig dbConfig = mock(DbConfig.class);
    private final QueryResolver queryResolver = mock(QueryResolver.class);
    private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void returnsDebugInfo() throws Exception
    {
        when(message.getPayload()).thenReturn(getPlanetNames());
        when(event.getMessage()).thenReturn(message);

        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        when(queryResolver.resolve(any(DbConnection.class), any(MuleEvent.class))).thenReturn(new Query(QUERY_TEMPLATE1)).thenReturn(new Query(QUERY_TEMPLATE2));

        final DynamicBulkUpdateMessageProcessor dynamicBulkUpdateMessageProcessor = new DynamicBulkUpdateMessageProcessor(dbConfigResolver, queryResolver, null, NOT_SUPPORTED, Collections.singletonList(QueryType.UPDATE));
        dynamicBulkUpdateMessageProcessor.setMuleContext(muleContext);

        final List<FieldDebugInfo<?>> debugInfo = dynamicBulkUpdateMessageProcessor.getDebugInfo(event);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(1));

        assertThat(debugInfo, hasItem(objectLike(QUERIES_DEBUG_FIELD, List.class, createExpectedQueryMatchers())));
    }

    @Test
    public void returnsErrorDebugInfoWhenCannotResolveQueries() throws Exception
    {
        when(message.getPayload()).thenReturn(getPlanetNames());
        when(event.getMessage()).thenReturn(message);

        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        final QueryResolutionException queryResolutionException = new QueryResolutionException("Error");
        when(queryResolver.resolve(any(DbConnection.class), any(MuleEvent.class))).thenThrow(queryResolutionException);

        final DynamicBulkUpdateMessageProcessor dynamicBulkUpdateMessageProcessor = new DynamicBulkUpdateMessageProcessor(dbConfigResolver, queryResolver, null, NOT_SUPPORTED, Collections.singletonList(QueryType.UPDATE));
        dynamicBulkUpdateMessageProcessor.setMuleContext(muleContext);

        final List<FieldDebugInfo<?>> debugInfo = dynamicBulkUpdateMessageProcessor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(fieldLike(QUERIES_DEBUG_FIELD, List.class, queryResolutionException)));
    }

    private List<Matcher<FieldDebugInfo<?>>> createExpectedQueryMatchers()
    {
        final List<Matcher<FieldDebugInfo<?>>> queryMatchers = new ArrayList<>();
        queryMatchers.add(createQueryFieldDebugInfoMatcher(QUERY1, QUERY_TEMPLATE1));
        queryMatchers.add(createQueryFieldDebugInfoMatcher(QUERY2, QUERY_TEMPLATE2));

        return queryMatchers;
    }

    private static List<String> getPlanetNames()
    {
        List<String> planetNames = new ArrayList<>();
        planetNames.add("EARTH");
        planetNames.add("MARS");

        return planetNames;
    }
}