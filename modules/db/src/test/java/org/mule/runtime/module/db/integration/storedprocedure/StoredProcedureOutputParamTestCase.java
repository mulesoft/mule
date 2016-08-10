/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureOutputParamTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureOutputParamTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource,
              testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/storedprocedure/stored-procedure-out-param-config.xml"};
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        final MuleEvent responseEvent = flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();

        final MuleMessage response = responseEvent.getMessage();
        assertThat(response.getPayload(), is(instanceOf(Map.class)));
        Map payload = (Map) response.getPayload();
        // Apparently Derby has a bug: when there are no resultset returned, then
        // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
        //assertThat(payload.size(), equalTo(2));
        //assertThat((Integer) payload.get("updateCount1"), equalTo(0));
        // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)
        assertThat("3", CoreMatchers.equalTo(payload.get("count").toString()));
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        testDatabase.createStoredProcedureCountRecords(getDefaultDataSource());
    }
}
