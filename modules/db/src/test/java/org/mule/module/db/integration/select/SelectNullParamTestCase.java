/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.api.MuleEvent;
import org.mule.api.debug.ObjectFieldDebugInfo;
import org.mule.api.debug.SimpleFieldDebugInfo;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;
import org.mule.module.db.internal.processor.SelectMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectNullParamTestCase extends AbstractDbIntegrationTestCase
{

    public SelectNullParamTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/select/select-null-param-config.xml"};
    }

    @Test
    public void testOverrideParamWithNullValue() throws Exception
    {
        SimpleFieldDebugInfo paramInfo = getParams("overrideParamWithNullValue").get(0);
        assertThat(paramInfo.getName(), is("position"));
        assertThat(paramInfo.getValue(), is(nullValue()));
    }

    @Test
    public void testImplicitParamWithNullValue() throws Exception
    {
        SimpleFieldDebugInfo paramInfo = getParams("implicitNullValue").get(0);
        assertThat(paramInfo.getName(), is("position"));
        assertThat(paramInfo.getValue(), is(nullValue()));
    }

    private List<SimpleFieldDebugInfo> getParams(String flowName)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        assertThat(messageProcessors.get(0), instanceOf(SelectMessageProcessor.class));
        AbstractDbMessageProcessor selectMessageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);
        MuleEvent event = mock(MuleEvent.class);
        List debugInfo = selectMessageProcessor.getDebugInfo(event);
        ObjectFieldDebugInfo paramsInfo = (ObjectFieldDebugInfo) debugInfo.get(2);
        return (List) paramsInfo.getValue();
    }

}
