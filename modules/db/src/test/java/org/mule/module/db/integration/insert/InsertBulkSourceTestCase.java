/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.insert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertBulkSourceTestCase extends AbstractDbIntegrationTestCase
{

    public InsertBulkSourceTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/insert/insert-bulk-source-config.xml"};
    }

    @Test
    public void insertsInBulkModeFromCustomSource() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://insertBulkCustomSource", TEST_MESSAGE, null);

        assertBulkInsert(response.getPayload());
    }

    private void assertBulkInsert(Object payload) throws SQLException
    {
        assertTrue(payload instanceof int[]);
        int[] counters = (int[]) payload;
        assertEquals(2, counters.length);
        assertThat(counters[0], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));
        assertThat(counters[1], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));

        assertPlanetRecordsFromQuery("Pluto", "Saturn");
    }
}