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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.NOT_SUPPORTED;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_SET_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERY_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.module.db.internal.resolver.query.QueryResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

@SmallTest
public class PreparedBulkUpdateMessageProcessorDebugInfoTestCase extends AbstractMuleTestCase
{

    public static final String QUERY_SQL = "update PLANET set NAME='Mercury' where NAME=?";
    public static final QueryTemplate QUERY_TEMPLATE_WITH_NAMED_PARAM = new QueryTemplate(QUERY_SQL, UPDATE, Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, null, null)));
    public static final String PARAM1 = PARAM_DEBUG_FIELD_PREFIX + 1;
    public static final String PARAM_SET1 = PARAM_SET_DEBUG_FIELD_PREFIX + 1;
    public static final String PARAM_SET2 = PARAM_SET_DEBUG_FIELD_PREFIX + 2;

    private final MuleEvent event = mock(MuleEvent.class);
    private final MuleMessage message = mock(MuleMessage.class);
    private final DbConnection connection = mock(DbConnection.class);
    private final DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);
    private final DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
    private final DbConfig dbConfig = mock(DbConfig.class);
    private final QueryResolver queryResolver = mock(QueryResolver.class);
    private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void returnDebugInfo() throws Exception
    {
        List<String> planetNames = new ArrayList<>();
        planetNames.add(EARTH.getName());
        planetNames.add(MARS.getName());

        when(message.getPayload()).thenReturn(planetNames);
        when(event.getMessage()).thenReturn(message);

        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        when(queryResolver.resolve(argThat(equalTo(connection)), any(MuleEvent.class))).thenReturn(new Query(QUERY_TEMPLATE_WITH_NAMED_PARAM));

        final ParamValueResolver paramValueResolver = mock(ParamValueResolver.class);
        when(paramValueResolver.resolveParams(any(MuleEvent.class), any(List.class))).thenReturn(Collections.singletonList(new QueryParamValue(null, EARTH.getName()))).thenReturn(Collections.singletonList(new QueryParamValue(null, MARS.getName())));

        PreparedBulkUpdateMessageProcessor processor = new PreparedBulkUpdateMessageProcessor(dbConfigResolver, queryResolver, null, NOT_SUPPORTED, Collections.singletonList(UPDATE), paramValueResolver);
        processor.setMuleContext(muleContext);

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(2));


        List<Matcher<FieldDebugInfo<?>>> fieldMatchers = new ArrayList<>();
        fieldMatchers.add(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, QUERY_SQL));
        fieldMatchers.add(fieldLike(TYPE_DEBUG_FIELD, String.class, UPDATE.toString()));
        assertThat(debugInfo, hasItem(objectLike(QUERY_DEBUG_FIELD, Query.class, fieldMatchers)));
        assertThat(debugInfo, hasItem(objectLike(INPUT_PARAMS_DEBUG_FIELD, List.class, createExpectedParamSetMatchers())));
    }

    private List<Matcher<FieldDebugInfo<?>>> createExpectedParamSetMatchers()
    {
        final List<Matcher<FieldDebugInfo<?>>> paramSets = new ArrayList<>();

        final List<Matcher<FieldDebugInfo<?>>> paramSet1 = new ArrayList<>();
        paramSet1.add(fieldLike(PARAM1, String.class, EARTH.getName()));
        paramSets.add(objectLike(PARAM_SET1, List.class, paramSet1));

        final List<Matcher<FieldDebugInfo<?>>> paramSet2 = new ArrayList<>();
        paramSet2.add(fieldLike(PARAM1, String.class, MARS.getName()));
        paramSets.add(objectLike(PARAM_SET2, List.class, paramSet2));
        return paramSets;
    }
}