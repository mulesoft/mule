/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.mule.tck.testmodels.fruit.Apple;

import java.awt.Color;
import java.io.Serializable;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueMessageTypesTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-queue-message-types.xml";
    }

    @Test
    public void testTextMessage() throws Exception
    {
        dispatchMessage("TEST MESSAGE");
        receiveMessage("TEST MESSAGE");
        receive(scenarioNotReceive);
    }

    @Test
    public void testNumberMessage() throws Exception
    {
        dispatchMessage(25.75);
        receiveMessage(25.75);
        receive(scenarioNotReceive);
    }

    @Test
    public void testBinaryMessage() throws Exception
    {
        byte[] bytes = new byte[] {'\u0000', '\u007F', '\u0033', '\u007F', '\u0055'};
        dispatchMessage(bytes);
        receiveMessage(bytes);
        receive(scenarioNotReceive);
    }

    @Test
    public void testJdkObjectMessage() throws Exception
    {
        Serializable obj = new Color(0);
        dispatchMessage(obj);
        receiveMessage(obj);
        receive(scenarioNotReceive);
    }

    @Test
    public void testCustomObjectMessage() throws Exception
    {
        Serializable obj = new Apple();
        dispatchMessage(obj);
        receiveMessage(obj);
        receive(scenarioNotReceive);
    }

}
