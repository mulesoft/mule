/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.integration.update;

import static junit.framework.Assert.assertTrue;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class TruncateTableTestCase extends AbstractDbIntegrationTestCase
{

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    public TruncateTableTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/truncate-table-config.xml"};
    }

    @Test
    public void truncateTable() throws Exception
    {
        runFlow("truncateTable");
        List<Map<String, String>> result = selectData("select * from PLANET", getDefaultDataSource());
        assertTrue(result.isEmpty());
    }
}
