/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Planet;
import org.mule.module.db.integration.model.Record;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectWithAliasTestCase extends AbstractDbIntegrationTestCase
{

    public static final String NAME_FIELD_ALIAS = "PLANETNAME";

    public SelectWithAliasTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-with-alias-config.xml"};
    }

    @Test
    public void returnsAliasInResultSet() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://usesAlias", TEST_MESSAGE, null);

        assertMessageContains(response, getExpectedRecords());
    }

    public Record[] getExpectedRecords()
    {
        return new Record[] {
                new Record(new Field(NAME_FIELD_ALIAS, Planet.VENUS.getName())),
                new Record(new Field(NAME_FIELD_ALIAS, Planet.EARTH.getName())),
                new Record(new Field(NAME_FIELD_ALIAS, Planet.MARS.getName())),
        };
    }
}
