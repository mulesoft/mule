/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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

/**
 * Testing durable topic with XA transactions
 */
public class JmsMuleSideDurableTopicXATxTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String CONNECTOR1_NAME = "jmsConnectorC1";

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-muleside-durable-topic-xa-tx.xml";
    }

    @Test
    public void testMuleXaTopic() throws Exception
    {
        // There is a need to guarantee that XaMessageTopicReceiver connected to
        // topic
        Thread.sleep(5000);

        MuleMessage result;
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", DEFAULT_INPUT_MESSAGE, null);
        result = client.request("vm://out", getTimeout());
        assertNotNull(result);
        result = client.request("vm://out", getTimeout());
        assertNotNull(result);
        result = client.request("vm://out", getTimeout());
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
        logger.info("Received " + result.getPayload());
        result = client.request("vm://out", getTimeout());
        assertNotNull(result);
        logger.info("Received " + result.getPayload());
        result = client.request("vm://out", getSmallTimeout());
        assertNull(result);
    }
}
