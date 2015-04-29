/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class MergeTestCase extends AbstractDbIntegrationTestCase
{

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    public MergeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/merge-config.xml"};
    }

    @Test
    public void mergesTables() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://merge", TEST_MESSAGE, null);

        assertMergeResult(response);
    }

    private void assertMergeResult(MuleMessage response) throws SQLException
    {
        assertThat((Integer) response.getPayload(), equalTo(3));
        
        List<Map<String, String>> result = selectData("select * from PLANET order by ID", getDefaultDataSource());
        assertRecords(result, createRecord(2), createRecord(3), createRecord(4));
    }

    private Record createRecord(int pos)
    {
        return new Record(new Field("NAME", "merged"), new Field("POSITION", pos));
    }
}


