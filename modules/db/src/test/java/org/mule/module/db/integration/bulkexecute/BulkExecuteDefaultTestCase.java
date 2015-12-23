/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.bulkexecute;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class BulkExecuteDefaultTestCase extends AbstractBulkExecuteTestCase
{

    public BulkExecuteDefaultTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/bulkexecute/bulk-execute-default-config.xml"};
    }

    @Test
    public void updatesDataRequestResponse() throws Exception
    {
        final MuleEvent responseEvent = runFlow("bulkUpdateRequestResponse", TEST_MESSAGE);

        final MuleMessage response = responseEvent.getMessage();
        assertBulkModeResult(response.getPayload());
    }

    @Test
    public void testOneWay() throws Exception
    {
        runFlowAsync("bulkUpdateOneWay", TEST_MESSAGE);

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://testOut", RECEIVE_TIMEOUT);

        assertBulkModeResult(response.getPayload());
    }

}
