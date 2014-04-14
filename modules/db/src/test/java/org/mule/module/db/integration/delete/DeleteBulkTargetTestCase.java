/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.delete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.module.db.integration.DbTestUtil.assertExpectedUpdateCount;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.integration.model.Planet.VENUS;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteBulkTargetTestCase extends AbstractDbIntegrationTestCase
{

    public DeleteBulkTargetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/delete/delete-bulk-target-config.xml"};
    }

    @Test
    public void usesCustomTarget() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        List<String> planetNames = new ArrayList<String>();
        planetNames.add(VENUS.getName());
        planetNames.add(MARS.getName());

        MuleMessage response = client.send("vm://deleteCustomTarget", planetNames, null);

        assertBulkDelete(response.getInboundProperty("updateCount"));
    }

    private void assertBulkDelete(Object payload) throws SQLException
    {
        assertTrue(payload instanceof int[]);
        int[] counters = (int[]) payload;
        assertEquals(2, counters.length);
        assertExpectedUpdateCount(1, counters[0]);
        assertExpectedUpdateCount(1, counters[1]);

        assertDeletedPlanetRecords("Pluto", "Venus");
    }
}