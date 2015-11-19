/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
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
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureTargetTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureTargetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/storedprocedure/stored-procedure-target-config.xml"};
    }

    @Test
    public void usesCustomTarget() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://storedProcedureCustomTarget", TEST_MESSAGE, null);

        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE));
        assertThat(response.getInboundProperty("statementResult"), is(instanceOf(Map.class)));
        verifyUpdatedRecord();
    }

    private void verifyUpdatedRecord() throws SQLException
    {
        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
        testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
    }

}
