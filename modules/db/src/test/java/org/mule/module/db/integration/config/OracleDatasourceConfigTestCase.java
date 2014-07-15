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
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.OracleTestDatabase;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleDatasourceConfigTestCase extends AbstractDbIntegrationTestCase
{

    public OracleDatasourceConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/simple-select-config.xml"};
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        if (TestDbConfig.getOracleResource().isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.singletonList(new Object[] {"integration/config/oracle-datasource-config.xml", new OracleTestDatabase()});
        }
    }

    @Test
    public void acceptsDatasourceWithoutUserAndPassword() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://simpleSelect", TEST_MESSAGE, null);

        assertMessageContains(response, getAllPlanetRecords());
    }
}
