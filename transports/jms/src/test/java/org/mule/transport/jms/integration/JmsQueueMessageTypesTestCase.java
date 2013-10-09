/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.tck.testmodels.fruit.Apple;

import java.awt.Color;
import java.io.Serializable;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueMessageTypesTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
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
