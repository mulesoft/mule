/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.junit.Test;

/**
 * Message is put to topic with two subscribers
 */
public class JmsTopicTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-topic.xml";
    }

    @Test
    public void testJmsTopic() throws Exception
    {
        // One message is sent.
        dispatchMessage();
        // The same message is read twice from the same JMS topic.
        receiveMessage();
        receiveMessage();
    }

    @Test
    public void testMultipleSend() throws Exception
    {
        // One message is sent.
        dispatchMessage();
        dispatchMessage();
        // The same message is read twice from the same JMS topic.
        receiveMessage();
        receiveMessage();
        receiveMessage();
        receiveMessage();
    }
}
