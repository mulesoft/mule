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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.junit.Test;

public class JmsHeaderReplyToTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsHeaderReplyToTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-header-replyto.xml";
    }

    @Override
    protected Message createJmsMessage(Object payload, Session session) throws JMSException
    {
        Message message = super.createJmsMessage(payload, session);
        Destination dest = session.createQueue(getJmsConfig().getOutboundDestinationName());
        message.setJMSReplyTo(dest);
        return message;
    }
    
    /**
     * @name TC-TRANSP-JMS-H-1
     * @description The JMSReplyTo header field contains a Destination supplied by a client when a message is sent.
     * It is the destination where a reply to the message should be sent.
     * A client can use the JMSCorrelationID header field to link one message with another.
     * A typical use is to link a response message with its request message.
     * @configuration Component contains only inbound endpoint
     * @test-procedure
     *  - Create a message and set JMSReplyTo header
     * - Send message to Queue, save messageId
     * - get processed message from Destination indicated in the JMSReplyTo header, JMSCorrelationId has to equal
     * stored messageId
     * @expected Output message is successful received, sent messageId is equal to received correlationId
     */
    @Test
    public void testTranspJmsH1() throws Exception
    {        
        Message input = send(DEFAULT_INPUT_MESSAGE);
        
        Message output = receive();
        assertEquals(input.getJMSMessageID(), output.getJMSCorrelationID());

        // No more messages
        assertNull(receiveNoWait());
    }
}
