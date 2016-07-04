/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.mule.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.module.db.integration.model.RegionManager.NORTHWEST_MANAGER;
import static org.mule.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.OracleTestDatabase;
import org.mule.module.db.integration.model.Record;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStructUdtTestCase extends AbstractDbIntegrationTestCase
{
    public SelectStructUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-udt-config.xml"};
    }


    @Test
    public void returnsMappedObject() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://returnsUDT", TEST_MESSAGE, null);

        assertRecords(response.getPayload(),
                      new Record(new Field("REGION_NAME", SOUTHWEST_MANAGER.getRegionName()), new Field("MANAGER_NAME", SOUTHWEST_MANAGER.getName()), new Field("DETAILS", SOUTHWEST_MANAGER.getContactDetails().asObjectArray())),
                      new Record(new Field("REGION_NAME", NORTHWEST_MANAGER.getRegionName()), new Field("MANAGER_NAME", NORTHWEST_MANAGER.getName()), new Field("DETAILS", NORTHWEST_MANAGER.getContactDetails().asObjectArray())));
    }
}
