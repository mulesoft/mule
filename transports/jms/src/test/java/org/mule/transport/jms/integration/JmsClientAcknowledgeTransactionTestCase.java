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

/**
 * Test jms using JmsClientAcknowledgeTransactionFactory
 */
public class JmsClientAcknowledgeTransactionTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsClientAcknowledgeTransactionTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-client-acknowledge-tx.xml";
    }

    @Override
    public int getAcknowledgeMode()
    {
        return Session.CLIENT_ACKNOWLEDGE;
    }

    @Test
    public void testJmsClientAcknowledgeTransaction() throws Exception
    {
        send();
        
        // Receive but don't acknowledge
        Message output = receive();
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, output);

        // Receive again and acknowledge
        output = receive(new MessagePostProcessor() 
        {
            public void postProcess(Session session, Message message) throws JMSException
            {
                message.acknowledge();
            }
        });
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, output);

        // No more messages
        output = receiveNoWait();
        assertNull(output);
    }
}
