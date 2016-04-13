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
import static org.mockito.Mockito.when;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

public abstract class AbstractParameterizedSingleQueryMessageProcessorDebugInfoTestCase extends AbstractSingleQueryMessageProcessorDebugInfoTestCase
{

    public static final String NAME_PARAM = "name";
    public static final String POSITION_PARAM = "position";
    public static final String PARAM1 = PARAM_DEBUG_FIELD_PREFIX + 1;
    public static final String PARAM2 = PARAM_DEBUG_FIELD_PREFIX + 2;

    @Test
    public void returnsDebugInfoWithNamedParameters() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        when(queryResolver.resolve(connection, event)).thenReturn(createQueryWithNamedParameters());

        AbstractSingleQueryDbMessageProcessor processor = createMessageProcessor();

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertQueryDebugInfo(debugInfo, NAME_PARAM, POSITION_PARAM);
    }

    @Test
    public void returnsDebugInfoWithInlinedParameters() throws Exception
    {
        when(dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED)).thenReturn(connection);

        when(dbConfigResolver.resolve(event)).thenReturn(dbConfig);
        when(dbConfig.getConnectionFactory()).thenReturn(dbConnectionFactory);

        when(queryResolver.resolve(connection, event)).thenReturn(createQueryWithInlinedParams());

        AbstractSingleQueryDbMessageProcessor processor = createMessageProcessor();

        final List<FieldDebugInfo<?>> debugInfo = processor.getDebugInfo(event);

        assertQueryDebugInfo(debugInfo, PARAM1, PARAM2);
    }

    protected void assertQueryDebugInfo(List<FieldDebugInfo<?>> debugInfo, String paramName1, String paramName2)
    {
        assertThat(debugInfo.size(), equalTo(3));
        assertThat(debugInfo, hasItem(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, getSqlText())));
        assertThat(debugInfo, hasItem(fieldLike(TYPE_DEBUG_FIELD, String.class, getQueryType().toString())));

        final List<Matcher<FieldDebugInfo<?>>> paramMatchers = new ArrayList<>();
        paramMatchers.add(fieldLike(paramName1, String.class, EARTH.getName()));
        paramMatchers.add(fieldLike(paramName2, String.class, EARTH.getPosition()));
        assertThat(debugInfo, hasItem(objectLike(INPUT_PARAMS_DEBUG_FIELD, List.class, paramMatchers)));
    }

    protected Query createQueryWithNamedParameters()
    {
        final List<QueryParam> params = new ArrayList<>();
        params.add(new DefaultInputQueryParam(1, null, EARTH.getName(), NAME_PARAM));
        params.add(new DefaultInputQueryParam(2, null, EARTH.getPosition(), POSITION_PARAM));
        final QueryTemplate queryTemplate = new QueryTemplate(getSqlText(), getQueryType(), params);
        return new Query(queryTemplate);
    }

    protected Query createQueryWithInlinedParams()
    {
        final List<QueryParam> params = new ArrayList<>();
        params.add(new DefaultInputQueryParam(1, null, EARTH.getName(), null));
        params.add(new DefaultInputQueryParam(2, null, EARTH.getPosition(), null));
        final QueryTemplate queryTemplate = new QueryTemplate(getSqlText(),
                                                              getQueryType(), params);
        return new Query(queryTemplate);
    }

}
