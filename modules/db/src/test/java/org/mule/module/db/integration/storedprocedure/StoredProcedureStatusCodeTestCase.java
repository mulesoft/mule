/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Ignore("Need to test against a DB that supports this scenario")
public class StoredProcedureStatusCodeTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureStatusCodeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/storedprocedure/stored-procedure-status-code-config.xml"};
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://multiplyInts", TEST_MESSAGE, null);

        assertThat(response.getPayload(), is(Map.class));
        Map payload = (Map) response.getPayload();
        // Apparently Derby has a bug: when there are no resultset returned, then
        // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
        //assertThat(payload.size(), equalTo(2));
        assertThat((Integer) payload.get("result"), equalTo(12));
        assertTrue(payload.containsKey("statusCode"));

        //TODO(pablo.kraan): assert that output property is set
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        //TODO(pablo.kraan): I think it should check for Db's supportsStoredFunctionsUsingCallSyntax
        testDatabase.createStoredProcedureMultiplyInts(getDefaultDataSource());
    }
}
