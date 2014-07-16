/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.bulkexecute;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class BulkExecuteFileTestCase extends AbstractBulkExecuteTestCase
{

    public BulkExecuteFileTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/bulkexecute/bulk-execute-file-config.xml"};
    }

    @Test
    public void readQueriesFromFile() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://bulkUpdateFile", TEST_MESSAGE, null);

        assertBulkModeResult(response.getPayload());
    }
}
