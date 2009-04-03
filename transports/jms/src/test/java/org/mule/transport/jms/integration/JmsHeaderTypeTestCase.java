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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.junit.Test;

public class JmsHeaderTypeTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsHeaderTypeTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-header-type.xml";
    }

    @Override
    protected Message createJmsMessage(Object payload, Session session) throws JMSException
    {
        Message message = super.createJmsMessage(payload, session);
        message.setJMSType("NATALI");
        return message;
    }
    
    /**
     * @name TC-TRANSP-JMS-H-2
     * @description The JMSType header field contains a message type identifier supplied by a client when a message is sent.
     * @configuration 2 Components contain inbound endpoint with filter by JMSType
     *  The first - OLGA
     * The second - NATALI
     * @test-procedure
     *  - Create messages and set JMSType
     * - Send messages to Queue
     * - get messages, Every message must be processed the specified component
     * @expected 2 messages are successful received, content is unique
     */
    @Test
    public void testTranspJmsH2() throws Exception
    {
        Message input = send(DEFAULT_INPUT_MESSAGE);
        
        assertPayloadEquals("NATALI !!!", receive());

        // No more messages
        assertNull(receiveNoWait());
    }
}
