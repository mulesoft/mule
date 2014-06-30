/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testIn", TEST_MESSAGE, null);

        assertTrue(response.getExceptionPayload().getException() instanceof MessagingException);
        assertThat(counter, equalTo(1));
    }
}
