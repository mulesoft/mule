/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectDynamicQueryDebugInfoTestCase extends AbstractDbIntegrationTestCase
{

    public SelectDynamicQueryDebugInfoTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] { "integration/select/select-dynamic-query-config.xml" };
    }

    @Test
    public void providesDebugInfo() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("selectDynamicQuery");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(1);

        final MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);
        muleEvent.getMessage().setInvocationProperty("tableName", "PLANET");
        final List<FieldDebugInfo<?>> debugInfo = queryMessageProcessor.getDebugInfo(muleEvent);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(3));
        assertThat(debugInfo, hasItem(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, "select * from PLANET order by ID")));
        assertThat(debugInfo, hasItem(fieldLike(TYPE_DEBUG_FIELD, String.class, "SELECT")));
        assertThat(debugInfo, hasItem(fieldLike(INPUT_PARAMS_DEBUG_FIELD, List.class, empty())));
    }
}
