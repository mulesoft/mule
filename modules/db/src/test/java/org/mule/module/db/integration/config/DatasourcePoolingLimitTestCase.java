/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import org.junit.Test;

public class DatasourcePoolingLimitTestCase extends AbstractDatasourcePoolingTestCase
{

    public DatasourcePoolingLimitTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/derby-minimum-pooling-db-config.xml", "integration/config/connection-pooling-config.xml"};
    }

    @Test
    public void limitsConnections() throws Exception
    {
        try
        {
            LocalMuleClient client = muleContext.getClient();

            client.dispatch("vm://testIn", TEST_MESSAGE, null);
            client.dispatch("vm://testIn", TEST_MESSAGE, null);

            MuleMessage response = client.request("vm://connectionError", RECEIVE_TIMEOUT);
            assertThat(response.getExceptionPayload().getException(), is(instanceOf(MessagingException.class)));
        }
        finally
        {
            connectionLatch.countDown();
        }
    }
}
