/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.module.db.integration.model.Planet.EARTH;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.integration.model.Planet.VENUS;
import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getAllRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.matcher.ContainsMessage;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.matcher.SupportsStoredFunctionsUsingCallSyntax;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.core.AllOf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureStreamingReturningResultsetTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureStreamingReturningResultsetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/storedprocedure/stored-procedure-streaming-returning-resultset-config.xml"};
    }

    @Test
    public void testOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://testOneWay", TEST_MESSAGE, null);

        List<MuleMessage> responses = new LinkedList<MuleMessage>();
        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        responses.add(response);
        response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        responses.add(response);
        response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        responses.add(response);

        assertEquals(3, responses.size());
        assertThat(responses, AllOf.allOf(ContainsMessage.mapPayloadWith("NAME", MARS.getName()), ContainsMessage.mapPayloadWith("NAME", EARTH.getName()), ContainsMessage.mapPayloadWith("NAME", VENUS.getName())));
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testRequestResponse", TEST_MESSAGE, null);

        assertMessageContains(response, getAllRecords());
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        assumeThat(getDefaultDataSource(), new SupportsStoredFunctionsUsingCallSyntax());
        testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
    }
}
