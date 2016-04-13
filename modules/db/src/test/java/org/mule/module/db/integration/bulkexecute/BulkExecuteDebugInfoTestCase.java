/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.bulkexecute;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.module.db.internal.debug.DbDebugInfoTestUtils.createQueryFieldDebugInfoMatcher;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERIES_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;
import org.mule.module.db.internal.processor.DbDebugInfoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class BulkExecuteDebugInfoTestCase extends AbstractDbIntegrationTestCase
{

    public static final String QUERY1 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 1;
    public static final String QUERY2 = DbDebugInfoUtils.QUERY_DEBUG_FIELD + 2;
    public static final QueryTemplate QUERY_TEMPLATE1 = new QueryTemplate("update PLANET set NAME='Mercury' where POSITION=0", UPDATE, Collections.<QueryParam>emptyList());
    public static final QueryTemplate QUERY_TEMPLATE2 = new QueryTemplate("update PLANET set NAME='Mercury' where POSITION=4", UPDATE, Collections.<QueryParam>emptyList());

    public BulkExecuteDebugInfoTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/bulkexecute/bulk-execute-default-config.xml"};
    }

    @Test
    public void returnsDebugInfo() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("bulkUpdateRequestResponse");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractDbMessageProcessor queryMessageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);

        final MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);

        final List<FieldDebugInfo<?>> debugInfo = queryMessageProcessor.getDebugInfo(muleEvent);

        assertThat(debugInfo, is(not(nullValue())));
        assertThat(debugInfo.size(), equalTo(1));

        final List<Matcher<FieldDebugInfo<?>>> queryMatchers = new ArrayList<>();
        queryMatchers.add(createQueryFieldDebugInfoMatcher(QUERY1, QUERY_TEMPLATE1));
        queryMatchers.add(createQueryFieldDebugInfoMatcher(QUERY2, QUERY_TEMPLATE2));

        assertThat(debugInfo, hasItem(objectLike(QUERIES_DEBUG_FIELD, List.class, queryMatchers)));
    }

}
