/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
