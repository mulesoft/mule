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
import static org.mule.module.db.internal.debug.DbDebugInfoTestUtils.createQueryFieldDebugInfoMatcher;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERIES_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERY_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

public class UpdateDynamicBulkDebugInfoTestCase extends UpdateBulkTestCase
{

    public static final String QUERY1 = QUERY_DEBUG_FIELD + 1;
    public static final String QUERY2 = QUERY_DEBUG_FIELD + 2;

    public UpdateDynamicBulkDebugInfoTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-dynamic-bulk-config.xml"};
    }

    @Test
    public void providesDebugInfo() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("updateBulk");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractDbMessageProcessor queryMessageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);

        List<String> planetNames = new ArrayList<>();
        planetNames.add("EARTH");
        planetNames.add("MARS");

        final MuleEvent muleEvent = getTestEvent(planetNames);

        final List<FieldDebugInfo<?>> debugInfo = queryMessageProcessor.getDebugInfo(muleEvent);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(1));
        assertThat(debugInfo, hasItem(objectLike(QUERIES_DEBUG_FIELD, List.class, createExpectedQueryMatchers())));
    }

    private List<Matcher<FieldDebugInfo<?>>> createExpectedQueryMatchers()
    {
        final List<Matcher<FieldDebugInfo<?>>> queriesDebugInfo = new ArrayList<>();
        queriesDebugInfo.add(createQueryFieldDebugInfoMatcher(QUERY1, new QueryTemplate("update PLANET set NAME='Mercury' where NAME='EARTH'", UPDATE, Collections.<QueryParam>emptyList())));
        queriesDebugInfo.add(createQueryFieldDebugInfoMatcher(QUERY2, new QueryTemplate("update PLANET set NAME='Mercury' where NAME='MARS'", UPDATE, Collections.<QueryParam>emptyList())));

        return queriesDebugInfo;
    }
}