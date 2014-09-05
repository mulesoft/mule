/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureSourceTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureSourceTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/storedprocedure/stored-procedure-source-config.xml"};
    }

    @Test
    public void usesCustomSource() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://storedProcedureCustomSource", TEST_MESSAGE, null);
        assertThat(response.getPayload(), is(instanceOf(Map.class)));
        Map payload = (Map) response.getPayload();
        assertThat(payload.size(), equalTo(1));
        int expectedUpdateCount = testDatabase instanceof DerbyTestDatabase ? 0 : 1;
        assertThat((Integer) payload.get("updateCount1"), equalTo(expectedUpdateCount));

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Pluto"), new Field("POSITION", 4)));
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
        testDatabase.createStoredProcedureParameterizedUpdateTestType1(getDefaultDataSource());
    }
}
