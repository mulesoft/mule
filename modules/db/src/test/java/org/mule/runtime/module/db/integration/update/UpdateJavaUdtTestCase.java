/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.update;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.module.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.runtime.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.runtime.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.OracleTestDatabase;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateJavaUdtTestCase extends AbstractDbIntegrationTestCase
{

    public UpdateJavaUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        List<Object[]> params = new LinkedList<>();
        if (!getOracleResource().isEmpty())
        {
            params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
        }

        if (!getDerbyResource().isEmpty())
        {
            params.add(getDerbyResource().get(0));
        }

        return params;
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-udt-config.xml"};
    }

    @Test
    public void updatesObject() throws Exception
    {
        final MuleEvent responseEvent = flowRunner("updatesObject").withPayload(TEST_MESSAGE).run();
        final MuleMessage response = responseEvent.getMessage();

        assertThat(response.getPayload(), Matchers.<Object>equalTo(SOUTHWEST_MANAGER.getContactDetails()));
    }
}