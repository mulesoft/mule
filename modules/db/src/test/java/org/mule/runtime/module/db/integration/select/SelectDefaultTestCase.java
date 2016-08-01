/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.select;

import static org.mule.runtime.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectDefaultTestCase extends AbstractDbIntegrationTestCase
{

    public SelectDefaultTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-default-config.xml"};
    }

    @Test
    public void testOneWay() throws Exception
    {
        flowRunner("defaultQueryOneWay").withPayload(TEST_MESSAGE).asynchronously().run();

        MuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://testOut", RECEIVE_TIMEOUT);

        assertMessageContains(response, getAllPlanetRecords());
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        final MuleEvent responseEvent = flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();

        final MuleMessage response = responseEvent.getMessage();
        assertMessageContains(response, getAllPlanetRecords());
    }
}
