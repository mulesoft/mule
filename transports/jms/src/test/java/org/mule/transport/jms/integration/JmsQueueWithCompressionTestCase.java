/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue using compression in between
 */
public class JmsQueueWithCompressionTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "integration/jms-queue-with-compression.xml";
    }

    @Test
    public void testJmsQueue() throws Exception
    {
        // Lets test it doesn't blow up with serialized objects
        dispatchMessage(new Apple());
        receiveMessage();
        receive(scenarioNotReceive);
    }

    @Test
    public void testMultipleSend() throws Exception
    {
        dispatchMessage();
        dispatchMessage();
        dispatchMessage();
        receiveMessage();
        receiveMessage();
        receiveMessage();
        receive(scenarioNotReceive);
    }
}
