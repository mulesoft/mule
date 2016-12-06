/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.api.MuleEvent;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.OracleTestDatabase;

import java.sql.Struct;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DbCreateStructTestCase extends AbstractDbFunctionTestCase
{

    public DbCreateStructTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/function/create-struct-udt-config.xml"};
    }

    @Test
    public void createsStruct() throws Exception
    {
        Object[] payload = SOUTHWEST_MANAGER.getContactDetails().asObjectArray();

        MuleEvent response = runFlow("createsStruct", payload);

        assertThat(((Struct) response.getMessage().getPayload()).getAttributes(), equalTo(SOUTHWEST_MANAGER.getContactDetails().asObjectArray()));
    }
}
