/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.oracle;

import javax.jms.JMSException;

import oracle.AQ.AQException;

import org.mule.test.integration.providers.jms.oracle.util.AQUtil;

/**
 * Tests the connector against a live Oracle database using standard JMS messages.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class StandardPayloadIntegrationTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml";
    }

    public void testCreateAndDropQueue() throws AQException, JMSException {
       AQUtil.createOrReplaceQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_RAW, "RAW");
       AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_RAW, /*force*/false);
    }

    public void testTextMessage() throws Exception {
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);

        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT, 2000).getPayloadAsString());

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }

    public void testI18NMessage() throws Exception {
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);

        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.I18N_MESSAGE, null);
        assertEquals(TestConfig.I18N_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT, 2000).getPayloadAsString());

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }
}
