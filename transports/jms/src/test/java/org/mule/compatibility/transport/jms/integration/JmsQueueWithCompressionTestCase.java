/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Message is sent to and received from simple queue using compression in between
 */
@Ignore("MULE-9628")
public class JmsQueueWithCompressionTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
