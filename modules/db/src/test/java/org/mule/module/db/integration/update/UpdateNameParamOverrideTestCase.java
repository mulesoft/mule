/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.junit.Assert.assertEquals;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateNameParamOverrideTestCase extends AbstractDbIntegrationTestCase
{

    public UpdateNameParamOverrideTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/update/update-name-param-override-config.xml"};
    }

    @Test
    public void usesDefaultParams() throws Exception
    {
        final MuleEvent responseEvent = runFlow("defaultParams", TEST_MESSAGE);

        final MuleMessage response = responseEvent.getMessage();
        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void usesOverriddenParams() throws Exception
    {
        final MuleEvent responseEvent = runFlow("overriddenParams", TEST_MESSAGE);

        final MuleMessage response = responseEvent.getMessage();
        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=2", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 2)));
    }

    public void usesInlineOverriddenParams() throws Exception
    {
        final MuleEvent responseEvent = runFlow("inlineOverriddenParams", TEST_MESSAGE);

        final MuleMessage response = responseEvent.getMessage();
        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
    }

    @Test
    public void usesParamsInInlineQuery() throws Exception
    {
        final MuleEvent responseEvent = runFlow("inlineQuery", TEST_MESSAGE);

        final MuleMessage response = responseEvent.getMessage();
        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void usesExpressionParam() throws Exception
    {
        Map<String, Object> props = new HashMap<>();
        props.put("type", 3);

        final DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST_MESSAGE, props, Collections.emptyMap(), Collections.emptyMap(), muleContext);
        final DefaultMuleEvent muleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, null);

        final MuleEvent responseEvent = runFlow("expressionParam", muleEvent);

        final MuleMessage response = responseEvent.getMessage();
        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
    }
}
