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

import org.junit.Test;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsQueueTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-queue.xml";
    }

    @Test
    public void testJmsQueue() throws Exception
    {
        send(DEFAULT_INPUT_MESSAGE);
        
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, receive());
        
        // No more messages
        assertNull(receiveNoWait());
    }

    @Test
    public void testMultipleSend() throws Exception
    {
        send(DEFAULT_INPUT_MESSAGE);
        send(DEFAULT_INPUT_MESSAGE);
        send(DEFAULT_INPUT_MESSAGE);
        
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, receive());
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, receive());
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, receive());

        // No more messages
        assertNull(receiveNoWait());
    }
}
