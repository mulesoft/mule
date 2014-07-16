/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.delete;

import static org.junit.Assert.assertEquals;
import static org.mule.module.db.integration.model.Planet.VENUS;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteDefaultTestCase extends AbstractDbIntegrationTestCase
{

    public DeleteDefaultTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/delete/delete-default-config.xml"};
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testRequestResponse", VENUS.getName(), null);

        assertEquals(1, response.getPayload());
    }

    @Test
    public void testOneWay() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://testOneWay", VENUS.getName(), null);

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertEquals(1, response.getPayload());
    }
}
