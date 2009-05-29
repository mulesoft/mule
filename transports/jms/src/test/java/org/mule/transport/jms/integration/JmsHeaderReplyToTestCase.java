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
