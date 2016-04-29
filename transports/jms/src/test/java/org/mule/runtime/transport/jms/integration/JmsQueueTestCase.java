/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-queue.xml";
    }

    @Test
    public void testJmsQueue() throws Exception
    {
        dispatchMessage();
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
