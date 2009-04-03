/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.tck.testmodels.fruit.Apple;

import java.awt.Color;
import java.io.Serializable;

import javax.jms.Message;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueMessageTypesTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsQueueMessageTypesTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-queue-message-types.xml";
    }

    @Test
    public void testTextMessage() throws Exception
    {
        send("TEST MESSAGE");
        Message output = receive();
        assertPayloadEquals("TEST MESSAGE", output);
        assertNull(receiveNoWait());
    }

    @Test
    public void testNumberMessage() throws Exception
    {
        send(25.75);
        Message output = receive();
        assertPayloadEquals(25.75, output);
        assertNull(receiveNoWait());
    }

    @Test
    public void testBinaryMessage() throws Exception
    {
        byte[] bytes = new byte[] {'\u0000', '\u007F', '\u0033', '\u007F', '\u0055'};
        send(bytes);
        Message output = receive();
        assertPayloadEquals(bytes, output);
        assertNull(receiveNoWait());
    }

    @Test
    public void testJdkObjectMessage() throws Exception
    {
        Serializable obj = new Color(0);        
        send(obj);
        Message output = receive();
        assertPayloadEquals(obj, output);
        assertNull(receiveNoWait());
    }

    @Test
    public void testCustomObjectMessage() throws Exception
    {
        Serializable obj = new Apple();
        send(obj);
        Message output = receive();
        assertPayloadEquals(obj, output);
        assertNull(receiveNoWait());
    }
}
