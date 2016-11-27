/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.insert;

import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.TestDbConfig.getResources;
import org.mule.api.MuleEvent;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertClobInputParamTestCase extends AbstractDbIntegrationTestCase
{

    public InsertClobInputParamTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/insert/insert-clob-input-param-config.xml"};
    }

    @Test
    public void usesStringOnImplicitParam() throws Exception
    {
        MuleEvent response = runFlow("usesStringOnImplicitParam", TEST_MESSAGE);

        assertThat(response.getMessage().getPayload(), Matchers.<Object>equalTo(TEST_MESSAGE));
    }

    @Test
    public void usesStringOnExplicitParam() throws Exception
    {
        MuleEvent response = runFlow("usesStringOnExplicitParam", TEST_MESSAGE);

        assertThat(response.getMessage().getPayload(), Matchers.<Object>equalTo(TEST_MESSAGE));
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
    }
}
