/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.junit.Test;

public class JmsMuleSideDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{
    public static final String CONNECTOR1_NAME = "jmsConnectorC1";

    public JmsMuleSideDurableTopicTestCase(JmsVendorConfiguration config)
    {
        super(config);
        setUseTopics(true);
        setPersistent(true);
        setClientId(getClass().getName());
    }
    
    protected String getConfigResources()
    {
        return "integration/jms-muleside-durable-topic.xml";
    }

    @Test
    public void testMuleDurableSubscriber() throws Exception
    {
        send();
        receive();
        receive();
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).stop();
        assertEquals(muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).isStarted(), false);
        logger.info(CONNECTOR1_NAME + " is stopped");
        send();
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).start();
        logger.info(CONNECTOR1_NAME + " is started");
        receive();
        receive();

    }
}
