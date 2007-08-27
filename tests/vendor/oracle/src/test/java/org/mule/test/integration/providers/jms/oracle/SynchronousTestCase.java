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

import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Makes sure the Oracle JMS connector does not fail when sending a synchronous message.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class SynchronousTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml";
    }

    public void setUp() throws Exception {
        super.setUp();
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);
    }

    public synchronized void tearDown() throws Exception {
        wait(2000);
        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);

    }

    public void testTextMessage() throws Exception {
        muleClient.send("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT, 2000).getPayloadAsString());
    }
}
