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
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractHostPortConfigTestCase extends AbstractDbIntegrationTestCase
{

    @Rule
    public SystemProperty databasePort = new SystemProperty("database.port", getDatabasePortPropertyValue());

    protected abstract String getDatabasePortPropertyValue();

    public AbstractHostPortConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/simple-select-config.xml"};
    }

    @Test
    public void usesDatasourceConfig() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://usesHostPortConfig", TEST_MESSAGE, null);

        assertMessageContains(response, getAllPlanetRecords());
    }
}
