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
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.integration.TestDbConfig;

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
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://defaultParams", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void usesOverriddenParams() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://overriddenParams", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=2", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 2)));
    }

    public void usesInlineOverriddenParams() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://inlineOverriddenParams", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
    }

    @Test
    public void usesParamsInInlineQuery() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://inlineQuery", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void usesExpressionParam() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("type", 3);
        MuleMessage response = client.send("vm://expressionParam", TEST_MESSAGE, props);

        assertEquals(1, response.getPayload());
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
    }
}
