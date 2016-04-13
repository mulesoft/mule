/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_SET_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERY_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateBulkParameterizedDebugInfoTestCase extends AbstractDbIntegrationTestCase
{

    public static final String PARAM1 = PARAM_DEBUG_FIELD_PREFIX + 1;
    public static final String PARAM_SET1 = PARAM_SET_DEBUG_FIELD_PREFIX + 1;
    public static final String PARAM_SET2 = PARAM_SET_DEBUG_FIELD_PREFIX + 2;

    public UpdateBulkParameterizedDebugInfoTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-bulk-config.xml"};
    }

    @Test
    public void providesDebugInfo() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("updateBulk");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractDbMessageProcessor queryMessageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);

        List<String> planetNames = new ArrayList<>();
        planetNames.add(EARTH.getName());
        planetNames.add(MARS.getName());

        final MuleEvent muleEvent = getTestEvent(planetNames);

        final List<FieldDebugInfo<?>> debugInfo = queryMessageProcessor.getDebugInfo(muleEvent);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(2));

        List<Matcher<FieldDebugInfo<?>>> queryMatcher = new ArrayList<>();
        queryMatcher.add(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, "update PLANET set NAME='Mercury' where NAME=?"));
        queryMatcher.add(fieldLike(TYPE_DEBUG_FIELD, String.class, UPDATE.toString()));

        assertThat(debugInfo, hasItem(objectLike(QUERY_DEBUG_FIELD, Query.class, queryMatcher)));
        assertThat(debugInfo, hasItem(objectLike(INPUT_PARAMS_DEBUG_FIELD, List.class, createExpectedParamSetMatchers())));
    }

    private List<Matcher<FieldDebugInfo<?>>> createExpectedParamSetMatchers()
    {
        final List<Matcher<FieldDebugInfo<?>>> paramSetMatchers = new ArrayList<>();

        final List<Matcher<FieldDebugInfo<?>>> paramSet1 = new ArrayList<>();
        paramSet1.add(fieldLike(PARAM1, String.class, EARTH.getName()));
        paramSetMatchers.add(objectLike(PARAM_SET1, List.class, paramSet1));

        final List<Matcher<FieldDebugInfo<?>>> paramSet2 = new ArrayList<>();
        paramSet2.add(fieldLike(PARAM1, String.class, MARS.getName()));
        paramSetMatchers.add(objectLike(PARAM_SET2, List.class, paramSet2));
        return paramSetMatchers;
    }
}
