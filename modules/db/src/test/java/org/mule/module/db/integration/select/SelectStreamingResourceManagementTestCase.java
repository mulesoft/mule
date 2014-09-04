/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.internal.result.resultset.ResultSetIterator;
import org.mule.transport.NullPayload;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStreamingResourceManagementTestCase extends AbstractDbIntegrationTestCase
{

    public SelectStreamingResourceManagementTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/derby-pooling-db-config.xml", "integration/select/select-streaming-resource-management-config.xml"};
    }

    @Test
    public void closesConnectionsWhenResultSetConsumed() throws Exception
    {
        doSuccessfulTestMessage();
        doSuccessfulTestMessage();
        doSuccessfulTestMessage();
    }

    private void doSuccessfulTestMessage() throws MuleException
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://selectStreaming", TEST_MESSAGE, null);

        assertThat(response.getPayload(), is(instanceOf(ResultSetIterator.class)));
        assertRecords(response.getInboundProperty("processedRecords"), getAllPlanetRecords());
    }

    @Test
    public void closesConnectionsOnProcessingError() throws Exception
    {
        doFailedMessageTest();
        doFailedMessageTest();
        doFailedMessageTest();
    }

    private void doFailedMessageTest() throws MuleException
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://selectStreamingError", TEST_MESSAGE, null);

        assertThat(response.getExceptionPayload(), notNullValue());
        assertThat(response.getExceptionPayload().getRootException().getMessage(), equalTo("Failing test on purpose"));
        assertThat(response.getPayload(), is(instanceOf(NullPayload.class)));
    }
}
