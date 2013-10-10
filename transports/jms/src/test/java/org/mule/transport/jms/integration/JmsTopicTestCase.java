/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * Message is put to topic with two subscribers
 */
public class JmsTopicTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
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
