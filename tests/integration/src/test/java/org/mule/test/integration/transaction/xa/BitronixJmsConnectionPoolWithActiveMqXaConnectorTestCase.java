/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 *
 */
public class BitronixJmsConnectionPoolWithActiveMqXaConnectorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-with-activemq-config.xml";
    }


    @Test
    public void connectWithUsernameAndPassword() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput", TEST_MESSAGE, null);
        assertNotNull(response);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
    }
}
