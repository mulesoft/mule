/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.module.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectMaxRowsTestCase extends AbstractDbIntegrationTestCase
{

    @Rule
    public SystemProperty maxRows = new SystemProperty("maxRows", "2");

    public SelectMaxRowsTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-max-rows-config.xml"};
    }

    @Test
    public void limitsRows() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://selectMaxRows", TEST_MESSAGE, null);

        assertMessageContains(response, getVenusRecord(), getEarthRecord());
    }

    @Test
    public void limitsStreamedRows() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://selectMaxStreamedRows", TEST_MESSAGE, null);

        assertMessageContains(response, getVenusRecord(), getEarthRecord());
    }
}
