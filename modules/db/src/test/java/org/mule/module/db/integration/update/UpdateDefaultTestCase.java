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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateDefaultTestCase extends AbstractDbIntegrationTestCase
{

    public UpdateDefaultTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/update/update-default-config.xml"};
    }

    @Test
    public void updatesDataRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://updateRequestResponse", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());
        verifyUpdatedRecord();
    }

    @Test
    public void updatesDataOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://updateOneWay", TEST_MESSAGE, null);

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertEquals(1, response.getPayload());

        verifyUpdatedRecord();
    }

    private void verifyUpdatedRecord() throws SQLException
    {
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }
}
