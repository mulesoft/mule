/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import org.junit.Test;

public abstract class AbstractDatabaseConfigTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractDatabaseConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/simple-select-config.xml"};
    }

    @Test
    public void configuresDatabaseSuccessfully() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://simpleSelect", TEST_MESSAGE, null);

        assertMessageContains(response, getAllPlanetRecords());
    }
}
