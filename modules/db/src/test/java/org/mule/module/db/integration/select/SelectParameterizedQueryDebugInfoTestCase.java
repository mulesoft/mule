/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
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
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;
import org.mule.module.db.internal.processor.DbDebugInfoUtils;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectParameterizedQueryDebugInfoTestCase extends AbstractDbIntegrationTestCase
{

    public static final String PARAM1 = DbDebugInfoUtils.PARAM_DEBUG_FIELD_PREFIX + 1;

    public SelectParameterizedQueryDebugInfoTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/inline-parameterized-query-config.xml"};
    }

    @Test
    public void providesDebugInfo() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("expressionParam");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(1);

        final MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);
        final String expectedPosition = "3";
        muleEvent.getMessage().setInvocationProperty("position", expectedPosition);
        final List<FieldDebugInfo<?>> debugInfo = queryMessageProcessor.getDebugInfo(muleEvent);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(3));
        assertThat(debugInfo, hasItem(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, "SELECT * FROM PLANET WHERE POSITION = ? AND NAME = 'Earth'")));
        assertThat(debugInfo, hasItem(fieldLike(TYPE_DEBUG_FIELD, String.class, "SELECT")));

        final List<Matcher<FieldDebugInfo<?>>> paramMatchers = new ArrayList<>();
        paramMatchers.add(fieldLike(PARAM1, String.class, expectedPosition));
        assertThat(debugInfo, hasItem(objectLike(INPUT_PARAMS_DEBUG_FIELD, List.class, paramMatchers)));
    }
}
