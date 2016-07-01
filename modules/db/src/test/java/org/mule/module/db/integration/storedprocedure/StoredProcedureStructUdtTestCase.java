/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.OracleTestDatabase;

import java.sql.Struct;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureStructUdtTestCase extends AbstractDbIntegrationTestCase
{
    public StoredProcedureStructUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        List<Object[]> params = new LinkedList<>();

        if (!getOracleResource().isEmpty())
        {
            params.add(new Object[] {"integration/config/oracle-unmapped-udt-db-config.xml", new OracleTestDatabase()});
        }

        return params;
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/storedprocedure/stored-procedure-udt-config.xml"};
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        testDatabase.createStoredProcedureGetManagerDetails(getDefaultDataSource());
    }

    @Test
    public void returnsObject() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://returnsObject", TEST_MESSAGE, null);

        assertThat(((Struct)response.getPayload()).getAttributes(), equalTo(SOUTHWEST_MANAGER.getContactDetails().asObjectArray()));
    }
}
