/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JmsXATransactionComponentTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String CONNECTOR1_NAME = "jmsConnector";

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-xa-tx-component.xml";
    }

    @Test
    public void testOneGlobalTx() throws Exception
    {
        MuleMessage result;
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", DEFAULT_INPUT_MESSAGE, null);
        result = client.request("vm://out", getTimeout());
        assertNotNull(result);
        result = client.request("vm://out", getSmallTimeout());
        assertNull(result);
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).stop();
        assertEquals(muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).isStarted(), false);
        logger.info(CONNECTOR1_NAME + " is stopped");
        client.dispatch("vm://in", DEFAULT_INPUT_MESSAGE, null);
        Thread.sleep(1000);
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).start();
        Thread.sleep(1000);
        logger.info(CONNECTOR1_NAME + " is started");
        result = client.request("vm://out", getTimeout());
        assertNotNull(result);
        result = client.request("vm://out", getSmallTimeout());
        assertNull(result);
    }
}
