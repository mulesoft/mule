/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.integration.model.Planet.VENUS;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.client.MuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.transport.NullPayload;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateBulkTestCase extends AbstractDbIntegrationTestCase
{

    private MuleClient client;

    public UpdateBulkTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = muleContext.getClient();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-bulk-config.xml"};
    }

    @Test
    public void updatesInBulkModeWithCollection() throws Exception
    {
        MuleMessage response = client.send("vm://updateBulk", getPlanetNames(), null);
        assertBulkModeResult(response);
    }

    @Test
    public void updatesInBulkModeWithIterator() throws Exception
    {
        MuleMessage response = client.send("vm://updateBulk", getPlanetNames().iterator(), null);
        assertBulkModeResult(response);
    }

    @Test
    public void updatesInBulkModeWithIterable() throws Exception
    {
        final List<String> planetNames = getPlanetNames();
        Iterable<String> iterable = new Iterable<String>()
        {
            @Override
            public Iterator<String> iterator()
            {
                return planetNames.iterator();
            }
        };

        MuleMessage response = client.send("vm://updateBulk", iterable, null);
        assertBulkModeResult(response);
    }

    @Test
    public void requiresSplittableType() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://updateBulk", TEST_MESSAGE, null);

        assertTrue(response.getPayload() instanceof NullPayload);
        assertNotNull(response.getExceptionPayload());
    }

    private void assertBulkModeResult(MuleMessage response) throws SQLException
    {
        assertTrue(response.getPayload() instanceof int[]);
        int[] counters = (int[]) response.getPayload();
        assertThat(counters[0], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));
        assertThat(counters[1], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));
        assertThat(counters[2], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));

        List<Map<String, String>> result = selectData("select * from PLANET order by ID", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 2)), new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)), new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    private List<String> getPlanetNames()
    {
        List<String> planetNames = new ArrayList<String>();
        planetNames.add(VENUS.getName());
        planetNames.add(MARS.getName());
        planetNames.add(EARTH.getName());
        return planetNames;
    }
}
