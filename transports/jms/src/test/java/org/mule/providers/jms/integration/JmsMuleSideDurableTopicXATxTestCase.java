/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;

import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

/**
 * Testing durable topic
 * with XA transactions
 */
public class JmsMuleSideDurableTopicXATxTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String CONNECTOR1_NAME = "jmsConnectorC1";

    protected String getConfigResources()
    {
        return "providers/activemq/jms-muleside-durable-topic-xa-tx.xml";
    }

    public void testMuleXaTopic() throws Exception
    {
        UMOMessage result = null;
        MuleClient client = new MuleClient();
        client.dispatch("vm://in", DEFAULT_INPUT_MESSAGE, null);
        result = client.receive("vm://out", TIMEOUT);
        assertNotNull(result);
        result = client.receive("vm://out", TIMEOUT);
        assertNotNull(result);
        result = client.receive("vm://out", SMALL_TIMEOUT);
        assertNull(result);

        managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).stop();
        assertEquals(managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).isStarted(), false);
        logger.info(CONNECTOR1_NAME + " is stopped");
        client.dispatch("vm://in", DEFAULT_INPUT_MESSAGE, null);
        Thread.sleep(1000);
        managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).start();
        Thread.sleep(1000);
        logger.info(CONNECTOR1_NAME + " is started");
        result = client.receive("vm://out", TIMEOUT);
        assertNotNull(result);
        result = client.receive("vm://out", TIMEOUT);
        assertNotNull(result);
        result = client.receive("vm://out", SMALL_TIMEOUT);
        assertNull(result);

    }
}
