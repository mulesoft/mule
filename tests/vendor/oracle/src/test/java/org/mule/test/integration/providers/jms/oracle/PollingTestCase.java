/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.oracle;

import org.mule.impl.model.ModelHelper;
import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Makes sure the Oracle AQ connector continues to process new incoming messages after
 * startup.
 */
public class PollingTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml, pass-through-config.xml";
    }

    public void setUp() throws Exception {
        super.setUp();
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT2, false);

        // We have to start the model _after_ the queues have been created, otherwise
        // the connector will try to create them dynamically.
        ModelHelper.getComponent("PassThrough").start();
    }

    public synchronized void tearDown() throws Exception {
        // We delete the receivers in order to drop all connections to the Oracle queues.
        // Otherwise we'll get an "ORA-00054: resource busy and acquire with NOWAIT specified"
        // exception when trying to drop the queues.
       managementContext.dispose();
        wait(2000);

        // TODO For some reason there are still open connections at this point which
        // prevent dropping the queues.
        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT2, /*force*/false);
        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }

    public synchronized void testTextMessage() throws Exception {
        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        wait(1000);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT2, 2000).getPayloadAsString());
        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        wait(1000);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT2, 2000).getPayloadAsString());
        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        wait(1000);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT2, 2000).getPayloadAsString());
    }
}
