/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.vendor.oracle;

import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getAllPlanetRecords;

import org.mule.api.MuleMessage;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleSelectWithTestCase extends AbstractDbIntegrationTestCase
{

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getOracleResource();
    }

    public OracleSelectWithTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/vendor/oracle/oracle-select-with-config.xml"};
    }

    @Test
    public void selectWith() throws Exception
    {
        MuleMessage response = runFlow("with").getMessage();

        assertMessageContains(response, getAllPlanetRecords());
    }
}
